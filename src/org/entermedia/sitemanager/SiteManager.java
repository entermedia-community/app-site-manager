package org.entermedia.sitemanager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.entermedia.autofailover.AutoFailoverManager;
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
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.HttpSharedConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SiteManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SiteManager.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected AutoFailoverManager fieldAutoFailoverManager;
	protected Searcher fieldDnsSearcher;
	private JSONObject fieldJson;
	int CPU_TIME_AVG = 10;
	
	private HttpSharedConnection httpconnection;
	private HttpClient fieldHttpClient;

	
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

	private JSONObject buildPushNotification(Data inInstance, MultiValued inReal, JSONObject json)
	{
		if (json == null)
		{
			json = new JSONObject();
			json.put("channel", "emergency_monitor");
			json.put("as_user", false);
			json.put("username", "csbotkey");
			json.put("text", "<!channel> Monitor Alert: ");
		}

		String message = "\n" + "" + inInstance.get("name") + " at " + inInstance.get("entermedia_servers") + " - " + inInstance.get("instanceurl") + ": ";
		if (!(boolean)inReal.getValue("isreachable"))
		{
			message += ". Health checks failed two consecutive time on, the instance might be down, please review it ASAP.";
		}
		if ((boolean)inReal.getValue("isssl"))
		{
			switch (inReal.get("sslstatus"))
			{
			case "torenew":
				message += " The instance's SSL certificate will expires in " + inReal.get("daystoexpiration") + " 	days" + ".";
				break;
			case "expired":
				message += " The instance's SSL certificate has expired on " + inReal.get("expirationdate") + ".";
				break;
			case "error":
				message += " Can't retrieve any SSL certificate for the following instance.";
				break;
			default:
				break;
			}
		}
		if (inReal.get("isdisk") != null && inReal.get("isdisk").compareTo("true") == 0)
		{
			message += " One or more disk partition is running out of space.";
		}
		json.put("text", (String)json.get("text") + message);
		return json;
	}

	private void sendPushNotification(JSONObject json)
	{
		String url = "https://slack.com/api/chat.postMessage";
		String token = getMediaArchive().getCatalogSettingValue("slack_api_key");

		try
		{
			if (token == null || token != null && token.isEmpty())
			{
				throw new Exception("No Slack user token set in catalogsettings");
			}
			HttpPost httpMethod = new HttpPost(url);
			httpMethod.setHeader("Authorization", "Bearer " + token);
			httpMethod.setEntity(new StringEntity(json.toString(), "UTF-8"));
			
			httpMethod.setHeader("Content-Type", "application/json; charset=utf-8");

			log.info("before call slack API");
			
//			HttpResponse response = getHttpConnection().getSharedClient().execute((HttpUriRequest) httpMethod);
			HttpResponse response = getHttpClient().execute((HttpUriRequest) httpMethod);
			
			log.info("aftercall slack API");
			StatusLine sl = response.getStatusLine();

			if (sl.getStatusCode() != 200)
			{
				throw new Exception("Can't send push notification: status code " + sl.getStatusCode());
			}
		}
		catch (Exception e)
		{
			log.error("Error sending push notification", e);
		}

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
			avg = new ArrayList(average);
		}
		else
		{
			avg = new ArrayList();
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

	private String buildURL(Data inInstance, String inCatalog,  String fileURL)
	{
		if (inInstance.get("instanceurl") == null)
		{
			throw new OpenEditException("Instance's URL or catalog missing");
		}
		String dns = inInstance.get("instanceurl");
		if (dns.endsWith("/"))
		{ 
			inInstance.setProperty("url", dns.substring(0, (dns.length() - 1)));
		}
		return inInstance.get("url") + "/" + inCatalog + fileURL;
	}

	private DiskSpace scanDisks(Data inInstance, Data inReal, int inPercent)
	{
		ArrayList<DiskPartition> partitions = new ArrayList<DiskPartition>();
		DiskSpace diskSpace = new DiskSpace(partitions);

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			Downloader downloader = new Downloader();

			String jsonString = downloader.downloadToString(buildURL(inInstance, inReal.get("catalog"), "/mediadb/services/system/systemstatus.json"));
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
		Searcher sites = inArchive.getSearcher("entermedia_sites_monitor");
		Collection<Data> sitestomonitor = sites.query().all().search();

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			
			//Get Instance Data
			Searcher instances = inArchive.getSearcher("entermedia_instances_monitored");
			Data instance = instances.query().exact("id", real.get("instanceid")).searchOne();
			if (instance == null) {
				continue;
			}

			if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareTo("ok") == 0)
			{
				try
				{
					ObjectMapper mapper = new ObjectMapper();
					Downloader downloader = new Downloader();

					String jsonString = downloader.downloadToString(buildURL(instance, real.get("catalog"), "/mediadb/services/system/softwareversions.json"));
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
	
	private ServerStats scanStats(MultiValued inReal, Data inInstance, ServerStats stats)
	{
		ArrayList<ServerStat> statList = new ArrayList<ServerStat>();

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			Downloader downloader = new Downloader();

			String jsonUrl = buildURL(inInstance, inReal.get("catalog"), "/mediadb/services/system/systemstatus.json");
			String jsonString = downloader.downloadToString(jsonUrl);

			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

			JSONArray results = (JSONArray) json.get("stats");
			for (Object statObj : results.toArray())
			{
				JSONObject statJSON = (JSONObject) statObj;

				ServerStat stat = mapper.readValue(statJSON.toJSONString(), ServerStat.class);
				statList.add(stat);
			}
			stats.build(statList);
			inReal.setValue("fullurl", jsonUrl);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		return stats;
	}

	private boolean checkServerStatus(MultiValued inReal, Data inInstance)
	{
		try
		{
			Downloader downloader = new Downloader();

			String jsonUrl = downloader.downloadToString(buildURL(inInstance, inReal.get("catalog"), "/mediadb/services/system/systemstatus.json"));
			JSONObject jsonString = (JSONObject) new JSONParser().parse(jsonUrl);

			JSONObject results = (JSONObject) jsonString.get("response");

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
	
	private Long scanOldSite(MultiValued inReal, Data inInstance)
	{
		HttpRequestBuilder builder = new HttpRequestBuilder();

		HttpPost postMethod = null;
		try
		{
			String fullpath = buildURL(inInstance, inReal.get("catalog"), "/emshare/index.html");
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
	
	public synchronized void scan(MediaArchive inArchive)
	{
		log.info("Starting scan");
		Searcher sites = inArchive.getSearcher("entermedia_instances_monitor");
		Collection<Data> sitestomonitor = sites.query().all().search();
		JSONObject json = null;
		boolean pushNotification = false;
		
		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			if (!real.getBoolean("monitoringenable"))
			{
				continue;
			}
			
			//Get Instance Data
			Searcher instances = inArchive.getSearcher("entermedia_instances");
			Data instance = instances.query().exact("id", real.get("instanceid")).searchOne();
			if (instance == null) {
				continue;
			}
			
			
			//DNS
			Data dns = (Data) getDnsSearcher().searchById((String)real.getValue("monitoredsitesdns"));
			//TODO: if DNS = nul create one
			String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			ServerStats stats = new ServerStats();

			
			if ((Integer)real.getValue("diskmaxusage") == null || ((Integer)real.getValue("diskmaxusage") != null && (Integer)real.getValue("diskmaxusage") == 0))
			{
				real.setValue("diskmaxusage", 95);
				sites.saveData(real, null);
			}

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
						if (scanOldSite(real, instance) != null)
						{
							if (real.getValue("isautofailover") != null )
							{
								if ((boolean)real.getValue("isautofailover"))
								{
									if ((boolean)dns.getValue("isfailover"))
									{
										leaveFailover(real, dns);
									}
								}
							}
							reachable = true;
							real.setValue("monitorstatuscolor", "GREEN");
							real.setValue("lastcheckfail", false);
						}
					}
					else 
					{
						stats = scanStats(real, instance, stats);
 						reachable = true;
						real.setValue("monitorstatuscolor", "GREEN");
						real.setValue("lastcheckfail", false);
						if (real.getValue("isautofailover") != null )
						{
							if ((boolean)real.getValue("isautofailover"))
							{
								if ((boolean)dns.getValue("isfailover"))
								{
									leaveFailover(real, dns);
								}
							}
						}
					}	
				}
				catch (Exception e)
				{
					//Too much logs
					//log.error("Connection failed on " + real.get("name"), e);
				}
				if (!reachable)
				{
					real.setValue("isreachable", false);
					real.setValue("lastcheckfail", true);
					String clusterColor = (String)real.getValue("monitorstatuscolor");
					
					if (clusterColor == null)
					{
						real.setValue("monitorstatuscolor", "GREEN");
						sites.saveData(real, null);
						clusterColor = (String)real.getValue("monitorstatuscolor");
					}
					if (clusterColor.compareTo("GREEN") == 0)
					{
						real.setValue("lastcheckfail", true);
					}
					
					setErrorType(disk, memory, heap, cpu, reachable, false/*
																			 * swap
																	 */, real);
					if (real.getValue("isautofailover") != null )
					{
						if ((boolean)real.getValue("isautofailover"))
						{

							if ((boolean)real.getValue("lastcheckfail") && !(boolean)dns.getValue("isfailover"))
							{
								// go failover
								enterFailover(real, dns, false);	
							}	
							else if (!(boolean)real.getValue("lastcheckfail") && !(boolean)dns.getValue("isfailover"))
							{
								// prep failover
								enterFailover(real, dns, true);
							}
						}
						else
						{
							if ((boolean)real.getValue("lastcheckfail") && clusterColor.compareTo("GREEN") == 0)
							{
								real.setValue("monitorstatuscolor", "YELLOW");
							}
							else if (clusterColor.compareTo("YELLOW") == 0)
							{
								real.setValue("monitorstatuscolor", "RED");
							}
						}
					}
					throw new OpenEditException("Server unreachable");
				}

				if (!checkServerStatus(real, instance))
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
					DiskSpace diskSpace = scanDisks(instance, real, map.get("DISK"));
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
						String clusterColor = (String)real.getValue("monitorstatuscolor");
						if (clusterColor == null)
						{
							real.setValue("monitorstatuscolor", "GREEN");
							sites.saveData(real, null);
							clusterColor = (String)real.getValue("monitorstatuscolor");
						}

						if (clusterColor.compareTo("GREEN") == 0)
						{
							real.setValue("monitorstatuscolor", "RED");
						}
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
				real.setProperty("monitoringstatus", "error");
				if (real.get("monitorstatuscolor") != null && real.get("monitorstatuscolor").compareTo("RED") == 0)
				{
					if (!Boolean.parseBoolean(real.get("mailsent")))
					{
						log.error("Error checking " + real.get("name"), e);
						if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
						{
							buildEmail(real, inArchive);
							json = buildPushNotification(instance, real, json);
							pushNotification = true;
						}
					}
				}
			}
		real.setProperty("lastchecked", dates);
		sites.saveData(real, null);
		}
		if (pushNotification && json != null)
		{
			try
			{
				sendPushNotification(json);
			}
			catch (Exception e)
			{
				log.error("Error sending slack notification", e);
			}
		}
		log.info("scan complete");
	}
	
	private void enterFailover(MultiValued real, Data dns, boolean isPrep)
	{
		AutoFailoverManager dnsManager = getAutoFailoverManager();

		if (isPrep)
		{
			// preparing to failover, lowering TTL 
			real.setValue("lastcheckfail", true);
			dnsManager.updateRecord(dns.get("name"), (int)dns.getValue("failoverttl"));
		}
		else 
		{
			// failover, TTL low, set failover url
			real.setValue("lastcheckfail", true);
			dns.setValue("isfailover", true);
			dnsManager.updateRecord(dns.get("name"), dns.get("failovercontent"));
		}
	}

	private void leaveFailover(MultiValued real, Data dns)
	{
		AutoFailoverManager dnsManager = getAutoFailoverManager();

		real.setValue("lastcheckfail", false);
		dns.setValue("isfailover", false);
		dnsManager.updateRecord(dns.get("name"), dns.get("originalcontent"), Integer.parseInt((String)dns.getValue("originalttl")));
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public AutoFailoverManager getAutoFailoverManager()
	{
		if (fieldAutoFailoverManager == null)
		{
			setAutoFailoverManager((AutoFailoverManager) getModuleManager().getBean(getCatalogId(), "autoFailoverManager"));	
		}
		return fieldAutoFailoverManager;
	}

	public void setAutoFailoverManager(AutoFailoverManager inAutoFailoverManager)
	{
		fieldAutoFailoverManager = inAutoFailoverManager;
	}

	public void setDnsSearcher(Searcher inDnsSearcher)
	{
		fieldDnsSearcher = inDnsSearcher;
	}

	private Searcher getDnsSearcher()
	{
		if (fieldDnsSearcher == null)
		{
			setDnsSearcher(getMediaArchive().getSearcher("monitoredsitesdns"));
		}
		return fieldDnsSearcher;
	}

	public JSONObject getJson()
	{
		return fieldJson;
	}

	public void setJson(JSONObject inJson)
	{
		fieldJson = inJson;
	}

	public HttpSharedConnection getHttpConnection()
	{
		if (httpconnection == null)
		{
			httpconnection = new HttpSharedConnection();
		}
		return httpconnection;
	}

	public HttpClient getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			RequestConfig globalConfig = RequestConfig.custom()
		            .setCookieSpec(CookieSpecs.DEFAULT)
		            .setConnectTimeout(15 * 1000)
		            .setSocketTimeout(120 * 1000)
		            .build();
			fieldHttpClient = HttpClients.custom()
		            .setDefaultRequestConfig(globalConfig)
		            .build();
		}
		return fieldHttpClient;
	}
	
}
