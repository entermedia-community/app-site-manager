package org.entermedia.sitemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.entermedia.diskpartitions.DiskPartition;
import org.entermedia.diskpartitions.DiskSpace;
import org.entermedia.serverstats.ServerStat;
import org.entermedia.serverstats.ServerStats;
import org.entermedia.softwareversions.SoftwareVersion;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.WebEmail;
import org.entermediadb.modules.update.Downloader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SiteManager.class);

	private String catalogId;
	private JSONObject fieldJson;
	private int MAX_ERROR = 10;
	int CPU_TIME_AVG = 10;

	private void sendResolved(MultiValued inReal, MediaArchive inArchive, String inDates)
	{
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-resolve.html";
		WebEmail templatemail = inArchive.createSystemEmail(inReal.get("notifyemail"), templatePage);

		templatemail.setSubject("[EM][" + inReal.get("name") + "] error resolved");
		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("monitored", inReal);
		objects.put("dates", inDates);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "false");
		inReal.setValue("alertcount", 0);
	}

	private void buildEmail(MultiValued inReal, MediaArchive inArchive)
	{
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-error.html";
		WebEmail templatemail = inArchive.createSystemEmail(inReal.get("notifyemail"), templatePage);

		templatemail.setSubject("[EM][" + inReal.get("name") + "] error detected");
		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("monitored", inReal);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "true");

	}

	private Map<String, Integer> getUsageMaxByClient(final Data inReal)
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>()
		{
			{
				Integer maxdiskusage = (Integer) inReal.getValue("diskmaxusage");
				Integer maxmemkusage = (Integer) inReal.getValue("memmaxusage");
				//CPU
				//HEAP

				put("MEMORY", maxmemkusage);
				put("DISK", maxdiskusage);
				//				put("CPU", maxcpukusage);
				//				put("HEAP", maxheapusage);
				//				put("SWAP", maxheapusage);
			}
		};
		return map;
	}

	private MultiValued buildData(MultiValued inReal, DiskSpace diskSpace, ServerStats inStats, boolean inMemory, boolean inCpu, boolean inHeap, boolean inDisk)
	{
		//		inReal.setProperty("heapused", inParser.getHeap());
		//		inReal.setProperty("heapusedpercent", inParser.getHeappercent());
		List<Object> avg = null;

		Collection<String> average = inReal.getValues("cpuload");
		if (average != null)
		{
			avg = new ArrayList<>(average);
		}
		else
		{
			avg = new ArrayList<>();
		}
		avg.add((Double)inStats.getCpu());
		if (avg.size() > CPU_TIME_AVG)
		{
			avg = avg.subList(avg.size() - 10, avg.size() - 1);
		}

		DecimalFormat decimalFormat = new DecimalFormat("##.###");
		inReal.setValue("loadaverage", decimalFormat.format(calculateAverage(avg)).toString() + "%");
		inReal.setValue("isreachable", true);

		
		Collection<String> partitionsUsage = new ArrayList<String>(); 
		for (DiskPartition partition : diskSpace.getPartitions())
		{
			partitionsUsage.add(partition.getName() + ": " + decimalFormat.format(partition.getUsagePercent()) + "%");
		}
		inReal.setValue("diskpartitionsusage", partitionsUsage);
		
		inReal.setValue("servermemoryfree", inStats.getMemoryfree());
		inReal.setValue("servermemorytotal", inStats.getMemorytotal());
		inReal.setValue("serverprocesscpu", inStats.getProcessCPU());
		inReal.setValue("serversystemcpu", inStats.getCpu());
		inReal.setValue("serverswaptotal", inStats.getSwapSize());
		inReal.setValue("serverswapfree", inStats.getSwapFree());
		inReal.setValue("totalassets", inStats.getTotalassets());
		inReal.setValue("clusterhealth", inStats.getClusterhealth());

		inReal.setProperty("ismemory", String.valueOf(inMemory));
		inReal.setProperty("iscpu", String.valueOf(inCpu));
		inReal.setProperty("isheap", String.valueOf(inHeap));

		inReal.setProperty("isdisk", String.valueOf(inDisk));
		return inReal;
	}

	private String buildURL(Data inReal, String fileURL)
	{
		if (inReal.get("url") == null)
		{
			throw new OpenEditException("Instance's URL or catalog missing");
		}
		String dns = inReal.get("url");
		if (dns.endsWith("/"))
		{ 
			inReal.setProperty("url", dns.substring(0, (dns.length() - 1)));
		}
		return inReal.get("url") + "/" + inReal.get("catalog") + fileURL;
	}

	private DiskSpace scanDisks(MultiValued inReal, int inPercent)
	{
		ArrayList<DiskPartition> partitions = new ArrayList<DiskPartition>();
		DiskSpace diskSpace = new DiskSpace(partitions);

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			Downloader downloader = new Downloader();

			String jsonString = downloader.downloadToString(buildURL(inReal, "/mediadb/services/system/systemstatus.json"));
			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

			JSONArray results = (JSONArray) json.get("partitions");
			for (Object partitionObj : results.toArray())
			{

				JSONObject partition = (JSONObject) partitionObj;
				DiskPartition diskPartiton = mapper.readValue(partition.toJSONString(), DiskPartition.class);

				diskPartiton.isOverloaded(inPercent);

				partitions.add(diskPartiton);

			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		diskSpace.setPartitions(partitions);
		return diskSpace;
	}

	public void scanSoftwareVersions(MediaArchive inArchive)
	{
		Searcher sites = inArchive.getSearcher("monitoredsites");
		Collection<Data> sitestomonitor = sites.query().all().search();

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);

			if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareTo("ok") == 0)
			{
				try
				{
					ObjectMapper mapper = new ObjectMapper();
					Downloader downloader = new Downloader();

					String jsonString = downloader.downloadToString(buildURL(real, "/mediadb/services/system/softwareversions.json"));
					JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

					JSONArray results = (JSONArray) json.get("results");
					for (Object versionObj : results.toArray())
					{

						JSONObject partition = (JSONObject) versionObj;
						SoftwareVersion version = mapper.readValue(partition.toJSONString(), SoftwareVersion.class);

						if (version.getName() != null)
						{
							real.setValue("version_" + version.getName(), version.getVersion() != null ? version.getVersion() : "");
						}
					}
				}
				catch (Exception e)
				{
					log.error("Cant' get software versions", e);
				}
			}
			sites.saveData(real, null);
		}
	}
	
	private ServerStats scanStats(ServerStats stats, MultiValued inReal)
	{
		ArrayList<ServerStat> statList = new ArrayList<ServerStat>();

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			Downloader downloader = new Downloader();

			String jsonString = downloader.downloadToString(buildURL(inReal, "/mediadb/services/system/systemstatus.json"));

			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

			JSONArray results = (JSONArray) json.get("stats");
			for (Object statObj : results.toArray())
			{
				JSONObject statJSON = (JSONObject) statObj;

				ServerStat stat = mapper.readValue(statJSON.toJSONString(), ServerStat.class);
				statList.add(stat);
			}
			stats.build(statList);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		return stats;
	}

	private boolean checkServerStatus(MultiValued inReal)
	{
		try
		{
			Downloader downloader = new Downloader();

			String jsonString = downloader.downloadToString(buildURL(inReal, "/mediadb/services/system/systemstatus.json"));
			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

			JSONObject results = (JSONObject) json.get("response");

			if (results.get("status") != null && results.get("status").equals("ok"))
			{
				return true;
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		return false;
	}

	private void setErrorType(boolean disk, boolean memory, boolean heap, boolean cpu, boolean reachable, boolean swap, MultiValued inReal)
	{
		if (inReal == null)
		{
			throw new OpenEditException("Can't get monitored site data");
		}

		ArrayList<String> list = new ArrayList<String>();
		if (disk)
		{
			list.add("disk");
		}
		if (memory)
		{
			list.add("memory");
		}
		if (heap)
		{
			list.add("heap");
		}
		if (cpu)
		{
			list.add("cpu");
		}
		if (!reachable)
		{
			list.add("reachable");
		}
		if (swap)
		{
			list.add("swap");
		}
		inReal.setValue("alerttype", list);
	}

	private Double calculateAverage(List<Object> inList)
	{
		Double sum = 0.0;
		if (!inList.isEmpty())
		{
			for (Object load : inList)
			{
				if (load != null)
				{
					sum += (Double)load;
				}
			}
			return sum / inList.size();
		}
		return sum;
	}
	
	public HttpClient getClient()
	{
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

		return httpClient;
	}
	
	private Long scanOldSite(MultiValued inReal)
	{
		HttpRequestBuilder builder = new HttpRequestBuilder();

		HttpPost postMethod = null;
		try
		{
			String fullpath = buildURL(inReal, "/emshare/index.html");
			postMethod = new HttpPost(fullpath);

			HashMap<String, String> props = new HashMap<String, String>();

			String emkey = inReal.get("entermediadbkey");
			if (emkey == null || (emkey != null && emkey.isEmpty()))
			{
				throw new OpenEditException("Missing entermedia.key on " + inReal.get("name"));
			}
			props.put("entermedia.key", emkey);

			HttpClient client = getClient();
			postMethod.setEntity(builder.build(props));

			Long startTime = System.currentTimeMillis();
			HttpResponse response = client.execute(postMethod);
			Long elapsedTime = System.currentTimeMillis() - startTime;

			int statuscode = response.getStatusLine().getStatusCode();
			if (statuscode == 200)
			{
				return elapsedTime;
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e.getMessage(), e);
		}
		return null;
	}
	
	public void scan(MediaArchive inArchive)
	{
		log.info("starting scan");
		Searcher sites = inArchive.getSearcher("monitoredsites");
		Collection<Data> sitestomonitor = sites.query().all().search();

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			ServerStats stats = new ServerStats();

			Map<String, Integer> map = getUsageMaxByClient(real);
			try
			{
				boolean disk = false;
				boolean memory = false;
				boolean heap = false;
				boolean cpu = false;
				boolean reachable = false;
				boolean isold;

				if (real.get("catalog") == null)
				{
					real.setValue("catalog", "assets");
				}
				isold = real.getBoolean("isold");
				try
				{
					if (isold == true)
					{
						if (scanOldSite(real) != null)
						{
							reachable = true;
						}
					}
					else 
					{
						stats = scanStats(stats, real);
						reachable = true;
					}
				}
				catch (Exception e)
				{
					log.error("Connection failed on " + real.get("name"), e);
				}
				if (!reachable)
				{
					real.setValue("isreachable", false);
					setErrorType(disk, memory, heap, cpu, reachable, false/*
																			 * swap
																			 */, real);
					throw new OpenEditException("Server unreachable");
				}

				if (!checkServerStatus(real))
				{
					setErrorType(disk, memory, heap, cpu, reachable, false/*
																			 * swap
																			 */, real);
					throw new OpenEditException("Server returns invalid status");
				}

				//TODO Calculate mem usage average over the past X Hour/minutes
				//memory = isOverloaded((Long)stats.getMemoryfree(), (Long)stats.getMemorytotal(), map.get("MEMORY"), false);

				if (!isold)
				{
					DiskSpace diskSpace = scanDisks(real, map.get("DISK"));
					disk = diskSpace.isOnePartitionOverloaded();
	
					
					if (disk == true)
					{
						real.setValue("partitions", diskSpace.getPartitions());
					}
	
					//				if (parser.getHeappercent() != null)
					//				{
					//					heap = new Double(stats.getHeappercent()) >= 90 ? true : false;
					//				}
					//				if (stats.getCpu() != null)
					//				{
					//					cpu = new Double(stats.getCpu()) >= 90 ? true : false;
					//
					//				}
	
					real = buildData(real, diskSpace, stats, memory, cpu, heap, disk);
	
					if (memory || heap || cpu || disk)
					{
						setErrorType(disk, memory, heap, cpu, reachable, false/*
																				 * swap
																				 */, real);
						throw new OpenEditException("Hardware overload");
					}
				}

				if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareToIgnoreCase("error") == 0)
				{
					if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
					{
						sendResolved(real, inArchive, dates);
					}
				}
				real.setValue("monitoringstatus", "ok");
				real.setValue("alerttype",new ArrayList<String>());
			}
			catch (Exception e)
			{
				log.error("Error checking " + real.get("name"), e);
				real.setProperty("monitoringstatus", "error");
				Integer alertcount = new Integer(0);
				if (real.get("alertcount") != null)
				{
					alertcount = new Integer(real.get("alertcount"));
				}

				if (alertcount <= MAX_ERROR)
				{
					alertcount += 1;
					real.setValue("alertcount", alertcount);
					if (!Boolean.parseBoolean(real.get("mailsent")))
					{
						if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
						{
							buildEmail(real, inArchive);
						}
					}
				}
			}
			real.setProperty("lastchecked", dates);
			sites.saveData(real, null);
		}
		log.info("scan complete");
	}

	@Override
	public void setCatalogId(String catalogId)
	{
		this.catalogId = catalogId;
	}

	public String getCatalogId()
	{
		return catalogId;
	}

	public JSONObject getJson()
	{
		return fieldJson;
	}

	public void setJson(JSONObject inJson)
	{
		fieldJson = inJson;
	}
}
