package org.entermedia.sitemanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermedia.autofailover.AutoFailoverManager;
import org.entermedia.diskpartitions.DiskPartition;
import org.entermedia.diskpartitions.DiskSpace;
import org.entermedia.serverstats.ServerStats;
import org.entermedia.softwareversions.SoftwareVersion;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.WebEmail;
import org.entermediadb.google.GoogleManager;
import org.entermediadb.modules.update.Downloader;
import org.entermediadb.net.HttpSharedConnection;
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
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SiteManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SiteManager.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected AutoFailoverManager fieldAutoFailoverManager;
	protected Exec fieldExec;
	protected Searcher fieldDnsSearcher;
	private JSONObject fieldJson;
	int CPU_TIME_AVG = 10;
	
	private HttpSharedConnection httpconnection;
	private HttpClient fieldHttpClient;

	
	private void sendEmailResolved(Data inInstance, MultiValued inReal, MediaArchive inArchive)
	{
		String notifyemail = "notifications@entermediadb.org";
		if (inArchive.getCatalogSettingValue("monitor_notify_email") != null) {
			notifyemail = inArchive.getCatalogSettingValue("monitor_notify_email");
		}
		if (inReal.get("notifyemail") != null && !inReal.get("notifyemail").isEmpty()) {
			notifyemail = inReal.get("notifyemail"); 
		}
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-resolve.html";
		WebEmail templatemail = inArchive.createSystemEmail(notifyemail, templatePage);

		templatemail.setSubject("[" + inInstance.get("name") + "] error resolved");
		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("monitored", inReal);
		objects.put("instance", inInstance);
		objects.put("dates", DateStorageUtil.getStorageUtil().getTodayForStorage());
		templatemail.send(objects);
		inReal.setProperty("mailsent", "false");
		inReal.setValue("alertcount", 0);
	}

	private void sendErrorNotification(Data inInstance, MultiValued inReal, MediaArchive inArchive) throws IOException
	{
		//Run trace result
		String monitoringurl = inReal.get("monitoringurl");
		String url = monitoringurl.substring(monitoringurl.indexOf("//") + 2);
		log.info("Monitoring URL: " + monitoringurl);
		log.info("Trace Route URL: " + url);
//		ArrayList<String> args = new ArrayList<String>();
//		args.add(url);
		/*
		 * args.add("https://unitednations-us-1.entermediadb.net"); args.add("m48");
		 */
//		log.info("Running 'traceroute' or checkserver.");
//		String traceresult = null;
//		/* bash* /home/entermedia/docker-doctor/checkserver.sh url m48 */
//		if ( monitoringurl.equals("https://unitednations-us-1.entermediadb.net") )
//		{
//			String[] env = {"PATH=/bin:/usr/bin/"};
//			String cmd = "checkserver.sh";  
//			Process process = Runtime.getRuntime().exec(cmd, env);
//		}
//		else if( monitoringurl.equals("https://unitednations-eu-1.entermediadb.net"))
//		{
//			String[] env = {"PATH=/bin:/usr/bin/"};
//			String cmd = "checkserver-eu.sh";  
//			Process process = Runtime.getRuntime().exec(cmd, env);
//		}
//		else if( monitoringurl.equals("https://unitednations-as-1.entermediadb.net"))
//		{
//			String[] env = {"PATH=/bin:/usr/bin/"};
//			String cmd = "checkserver-as.sh";  
//			Process process = Runtime.getRuntime().exec(cmd, env);
//		}
//		else {
//			
//			log.info("About to run checkserver...");
//			/* New way of running it , always run checkserver.sh for this testing phase.*/
//			String[] env = {"PATH=/bin:/usr/bin/"};
//			String cmd = "checkserver.sh";
//			Process process = Runtime.getRuntime().exec(cmd, env);
//			
//			/* Run traceroute */
//			 ExecResult trace = getExec().runExec("traceroute", args, true, 25000); 
//			 if(trace.getReturnValue() > 0) 
//			 { 
//				 traceresult = "Trace timed out! " + trace.getStandardError(); 
//			 } 
//			 else 
//			 { 
//				 traceresult = trace.getStandardOut(); 
//			 }
//			 
//			 
//		}
//		log.info("Trace Route: " + traceresult);
		
		// Send email
		String notifyemail = "notifications@entermediadb.org"; 
		if (inArchive.getCatalogSettingValue("monitor_notify_email") != null) {
			notifyemail = inArchive.getCatalogSettingValue("monitor_notify_email");
		}
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-error.html";
		WebEmail templatemail = inArchive.createSystemEmail(notifyemail, templatePage);

		templatemail.setSubject("[" + inInstance.get("name") + "] error detected!");
		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("monitored", inReal);
		objects.put("instance", inInstance);
//		objects.put("traceresult",traceresult);
		log.info("Sending email to:"+ notifyemail);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "true");
		
		//sends mobile push notifications for server errors.
		log.info("Sending error 'Push' notification.");
		sendEMPushNotification(inArchive,inInstance,inReal);
		
	}

//	private JSONObject buildPushNotification(Data inInstance, MultiValued inReal, JSONObject json)
//	{
//		if (json == null)
//		{
//			json = new JSONObject();
//			json.put("channel", "emergency_monitor");
//			json.put("as_user", false);
//			json.put("username", "csbotkey");
//			json.put("text", "<!channel> Monitor Alert: ");
//		}
//
//		String message = "\n" + "" + inInstance.get("name") + " at " + inInstance.get("entermedia_servers") + " - " + inInstance.get("instanceurl") + ": ";
//		if (!(boolean)inReal.getValue("isreachable"))
//		{
//			message += ". Health checks failed two consecutive time on, the instance might be down, please review it ASAP.";
//		}
//		if ((boolean)inReal.getValue("isssl"))
//		{
//			switch (inReal.get("sslstatus"))
//			{
//			case "torenew":
//				message += " The instance's SSL certificate will expires in " + inReal.get("daystoexpiration") + " 	days" + ".";
//				break;
//			case "expired":
//				message += " The instance's SSL certificate has expired on " + inReal.get("expirationdate") + ".";
//				break;
//			case "error":
//				message += " Can't retrieve any SSL certificate for the following instance.";
//				break;
//			default:
//				break;
//			}
//		}
//		if (inReal.get("isdisk") != null && inReal.get("isdisk").compareTo("true") == 0)
//		{
//			message += " One or more disk partition is running out of space.";
//		}
//		json.put("text", (String)json.get("text") + message);
//		return json;
//		return message;
//	}

//	private void sendPushNotification(JSONObject json)
//	{
//		String url = "https://slack.com/api/chat.postMessage";
//		String token = getMediaArchive().getCatalogSettingValue("slack_api_key");
//
//		try
//		{
//			if (token == null || token != null && token.isEmpty())
//			{
//				throw new Exception("No Slack user token set in catalogsettings");
//			}
//			HttpPost httpMethod = new HttpPost(url);
//			httpMethod.setHeader("Authorization", "Bearer " + token);
//			httpMethod.setEntity(new StringEntity(json.toString(), "UTF-8"));
//			
//			httpMethod.setHeader("Content-Type", "application/json; charset=utf-8");
//
//			log.info("before call slack API");
//			
////			HttpResponse response = getHttpConnection().getSharedClient().execute((HttpUriRequest) httpMethod);
//			HttpResponse response = getHttpClient().execute((HttpUriRequest) httpMethod);
//			
//			log.info("aftercall slack API");
//			StatusLine sl = response.getStatusLine();
//
//			if (sl.getStatusCode() != 200)
//			{
//				throw new Exception("Can't send push notification: status code " + sl.getStatusCode());
//			}
//		}
//		catch (Exception e)
//		{
//			log.error("Error sending push notification", e);
//		}
//
//	}
	
	//Sends mobile push notification for errors. Called in "sendErrorNotification()" found at the top of this page.
	private void sendEMPushNotification(MediaArchive mediaArchive,Data instanceData, MultiValued monitorData)
	{
		GoogleManager manager = (GoogleManager)mediaArchive.getBean("googleManager");
		
		//Create variables
		String collectionid = instanceData.get("librarycollection");
		String name = instanceData.get("name");
		String url = instanceData.get("monitoringurl");
		String inst_status = instanceData.get("instance_status");
		String mon_status = instanceData.get("monitoringstatus");
//
		//Create error message with variable and string concatination
		String message = "\n" + "" + name + " at " + instanceData.get("entermedia_servers") + " - " + url + ": ";
		if (!(boolean)monitorData.getValue("isreachable"))
		{
			message += ". Health checks failed two consecutive time on, the instance might be down, please review it ASAP.";
		}
		if ((boolean)monitorData.getValue("isssl"))
		{
			switch (monitorData.get("sslstatus"))
			{
			case "torenew":
				message += " The instance's SSL certificate will expires in " + monitorData.get("daystoexpiration") + " 	days" + ".";
				break;
			case "expired":
				message += " The instance's SSL certificate has expired on " + monitorData.get("expirationdate") + ".";
				break;
			case "error":
				message += " Can't retrieve any SSL certificate for the following instance.";
				break;
			default:
				break;
			}
		}
		
		String isdisk = monitorData.get("isdisk");
		if (isdisk != null && isdisk.compareTo("true") == 0)
		{
			message += " One or more disk partition is running out of space.";
		}
		//Create extra Map to pass important into notifyTopic
		Map extra = new HashMap();
//		extra.put("collectionid", collectionid);
		extra.put("instanceurl", url);
		extra.put("monitoringurl", url);
		extra.put("instancename", name);
		extra.put("instancestatus", inst_status);
		extra.put("monitoringstatus", mon_status);
		
		
		//Pass all required information into notifyTopic function
		manager.notifyTopic(collectionid,null,name,message,extra);
		
		
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

	private String buildURL(Data inMonitor, String inCatalog,  String fileURL)
	{
		String monitoringurl = inMonitor.get("monitoringurl"); 
		if ( monitoringurl== null)
		{
			throw new OpenEditException("Instance's URL or catalog missing");
		}
		String dns = monitoringurl;
		if (dns.endsWith("/"))
		{ 
			inMonitor.setProperty("monitoringurl", dns.substring(0, (dns.length() - 1)));
		}
		return monitoringurl + "/" + inCatalog + fileURL;
	}

//	private DiskSpace scanDisks(Data inInstance, Data inReal, int inPercent)
//	{
//		ArrayList<DiskPartition> partitions = new ArrayList<DiskPartition>();
//		DiskSpace diskSpace = new DiskSpace(partitions);
//
//		try
//		{
//			ObjectMapper mapper = new ObjectMapper();
//			Downloader downloader = new Downloader();
//
//			String jsonString = downloader.downloadToString(buildURL(inInstance, inReal.get("catalog"), "/mediadb/services/system/systemstatus.json"));
//			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
//
//			JSONArray results = (JSONArray) json.get("partitions");
//			for (Object partitionObj : results.toArray())
//			{
//
//				JSONObject partition = (JSONObject) partitionObj;
//				DiskPartition diskPartiton = mapper.readValue(partition.toJSONString(), DiskPartition.class);
//
//				diskPartiton.isOverloaded(inPercent);
//
//				partitions.add(diskPartiton);
//
//			}
//		}
//		catch (Exception e)
//		{
//			throw new OpenEditException(e);
//		}
//		diskSpace.setPartitions(partitions);
//		return diskSpace;
//	}

	/*
	 * public void scanSoftwareVersions(MediaArchive inArchive) { Searcher sites =
	 * inArchive.getSearcher("entermedia_sites_monitor"); Collection<Data>
	 * sitestomonitor = sites.query().all().search();
	 * 
	 * for (Data it : sitestomonitor) { MultiValued real = (MultiValued)
	 * sites.loadData(it);
	 * 
	 * //Get Instance Data Searcher instances =
	 * inArchive.getSearcher("entermedia_instances_monitored"); Data instance =
	 * instances.query().exact("id", real.get("instanceid")).searchOne(); if
	 * (instance == null) { continue; }
	 * 
	 * if (real.get("monitoringstatus") != null &&
	 * real.get("monitoringstatus").compareTo("ok") == 0) { try { ObjectMapper
	 * mapper = new ObjectMapper(); Downloader downloader = new Downloader();
	 * 
	 * String jsonString = downloader.downloadToString(buildURL(real,
	 * real.get("catalog"), "/mediadb/services/system/softwareversions.json"));
	 * JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
	 * 
	 * JSONArray results = (JSONArray) json.get("results"); for (Object versionObj :
	 * results.toArray()) {
	 * 
	 * JSONObject partition = (JSONObject) versionObj; SoftwareVersion version =
	 * mapper.readValue(partition.toJSONString(), SoftwareVersion.class);
	 * 
	 * if (version.getName() != null) { real.setValue("version_" +
	 * version.getName(), version.getVersion() != null ? version.getVersion() : "");
	 * } } } catch (Exception e) { log.error("Cant' get software versions", e); } }
	 * sites.saveData(real, null); } }
	 */
	
	protected ServerStats scanStats(MultiValued inMonitor, Data inInstance)
	{
		ServerStats stats = new ServerStats();

		ObjectMapper mapper = new ObjectMapper();
		Downloader downloader = new Downloader();
		stats.setReachable(true);

		String jsonUrl = buildURL(inMonitor, inMonitor.get("catalog"), "/mediadb/services/system/systemstatus.json");
		try
		{
			String jsonString = downloader.downloadToString(jsonUrl);
			JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

			JSONObject response = (JSONObject) json.get("response");

			if (response.get("status") != null && response.get("status").equals("ok"))
			{
				
				stats.setReachable(true);
				
				if(response.get("assetscount") != null)
				{
					stats.setTotalassets(response.get("assetscount"));
				}
				
				
			}
			else
			{
				stats.setReachable(false);
			}

		}
		catch (Exception e)
		{
			//throw new OpenEditException(e);
			log.error("Cant' get to " + jsonUrl, e);
			stats.setReachable(false);
		}
		return stats;
	}
/*
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
*/
	private void setErrorType(ServerStats inStats, MultiValued inReal)
	{
		if (inReal == null)
		{
			throw new OpenEditException("Can't get monitored site data");
		}

		ArrayList<String> list = new ArrayList<String>();
//		if (disk)
//		{
//			list.add("disk");
//		}
//		if (memory)
//		{
//			list.add("memory");
//		}
//		if (heap)
//		{
//			list.add("heap");
//		}
//		if (cpu)
//		{
//			list.add("cpu");
//		}
		if (!inStats.isReachable())
		{
			list.add("reachable");
		}
//		if (swap)
//		{
//			list.add("swap");
//		}
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
	/*
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
		*/
//	}
	
	public synchronized void scan(MediaArchive inArchive)
	{
		log.info("Starting monitor scan");
		Searcher sites = inArchive.getSearcher("entermedia_instances_monitor");
		Collection<Data> sitestomonitor = sites.query().exact("monitoringenable", "true").search();

		for (Data it : sitestomonitor)
		{
			try
			{
				scanSite(inArchive, it);
				getStats(inArchive, it);
			}
			catch( Throwable ex)
			{
				log.error("Could not scan site " + it ,ex);
			}
		}
		log.info("Scan complete on "+ sitestomonitor.size() +" sites.");
	}
	

	protected void scanSite(MediaArchive inArchive, Data inData)
	{
		Searcher sites = inArchive.getSearcher("entermedia_instances_monitor");
		MultiValued real = (MultiValued) sites.loadData(inData);
		
		//Get Instance Data
		Searcher instances = inArchive.getSearcher("entermedia_instances");
		Data instance = null;
		
		String instanceid = real.get("instanceid");
		if( instanceid != null)
		{
			instance = instances.query().exact("id", instanceid ).searchOne();
		}
		if (instance == null) 
		{
			log.error("Instance ID not valid " + real.getId());
			real.setValue("monitoringstatus", "error");
			real.setValue("alerttype", "invalidinstance");
			sites.saveData(real, null);
			return;
		}
		
		String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());

		if (real.get("catalog") == null)
		{
			real.setValue("catalog", "assets");
			sites.saveData(real, null);
		}
		
		/* Take care of blank monitoring statuses */
		if (real.get("monitoringstatus") == null)
		{
			real.setValue("monitoringstatus", "ok");
			sites.saveData(real, null);
		
		}
		
		ServerStats stats = scanStats(real, instance);
		try
		{
			//log.info(stats.isReachable());
			if (!stats.isReachable()) 
			{
				real.setValue("isreachable", false);
				real.setValue("lastcheckfail", true);
				String monstatus = real.get("monitoringstatus");
				log.error("Failing over. Monitoring status is " + monstatus);
				if(monstatus == null || monstatus.equals("ok") || monstatus.isEmpty() )
				{
					real.setValue("monitoringstatus", "error");
					setErrorType(stats,real);
					sites.saveData(real, null);
					enterFailover(real, instance, inArchive);	
				}	
			}
			else
			{
				if( real.get("monitoringstatus").equals("error")  )
				{
					real.setValue("monitoringstatus", "ok");
					real.setValue("lastcheckfail", false);
					real.setValue("isreachable", true);
					real.setValue("alerttype",null);
					sites.saveData(real, null);
					leaveFailover(real, instance, inArchive);
				}
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not scan: " + real.getName(), ex);
			real.addValue("alerttype", ex.getMessage()); //Should not happen
			sites.saveData(real, null);
		}

		real.setProperty("lastchecked", dates);
		sites.saveData(real, null);
		instance.setValue("monitoringstatus", real.getValue("monitoringstatus"));
		instances.saveData(instance, null);
		
	}
	
	private void getStats(MediaArchive inArchive, Data inData) throws JsonParseException, JsonMappingException, IOException {
		Searcher sites = inArchive.getSearcher("entermedia_instances_monitor");
		MultiValued instanceMonitor = (MultiValued) sites.loadData(inData);

		String catalog = (String) instanceMonitor.getValue("catalog");
		String emkey = (String) instanceMonitor.getValue("entermediadbkey");
		String url = (String) instanceMonitor.getValue("monitoringurl");
		Object dockerInstanceName = instanceMonitor.getValue("instancename");
		Object dockerInstanceNode = instanceMonitor.getValue("instancenode");
		
		String[] cmd = {
				"/bin/bash", "-c",
		        "/media/services/allchecks.sh " + catalog + " " + url + " " + emkey
		};

		String response = execCommand(cmd);
		log.info(response);

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(response, Map.class);
		
		Searcher instances = inArchive.getSearcher("entermedia_instances");
		Data instance = (Data) instances.searchById((String) instanceMonitor.getValue("instanceid"));
		Searcher servers = inArchive.getSearcher("entermedia_servers");
		Data server = (Data) servers.searchById((String) instance.getValue("entermedia_servers"));
		log.info("Scanning for Instance: " + instance.getName());
		
		String serverUrl = "http://" + (String) server.getValue("serverurl")  + "/stats.json";
		Map<String, Object> allStats = httpGetRequest(serverUrl);
		log.info("Scanning for serverUrl: " + serverUrl);		
		
		String nodeName = dockerInstanceName == null || dockerInstanceNode == null ? "" : dockerInstanceName.toString() + dockerInstanceNode.toString();
		if (nodeName.isEmpty()) {
			log.error("NodeName not configured on:" +instance.getName() + ", server: " + server.getName());
			return;
		}
		Map<String, Object> node = null;
		ArrayList<Map<String, Object>> nodes = (ArrayList<Map<String, Object>>) allStats.get("Nodes");
		log.info("Scanning for node: " + nodeName);
		for (int i =0; i < nodes.size(); i++) {
			Map<String, Object> serverNode = nodes.get(i);
			String serverName = (String) serverNode.get("Node");
			if (serverName.equals(nodeName)) {
				log.info("Found for node on URL: " + serverName);
				node = serverNode;
				break;
			}			
		}

		instanceMonitor.setValue("snapshotstatus", map.get("snapshotStatus"));
		instanceMonitor.setValue("lastsnapshot", map.get("lastSnapshot"));
		instanceMonitor.setValue("emserverversion", map.get("serverVersion"));

		if (node != null) {
			log.info("node found: " + node.get("Node"));
			instanceMonitor.setValue("emserverversion", (String) node.get("EMServerVersion"));
			instanceMonitor.setValue("version_ffmpeg", (String) node.get("FfmpegVersion"));
			instanceMonitor.setValue("version_ghostscript", (String) node.get("GhostScript"));
			instanceMonitor.setValue("version_imagemagick", (String) node.get("ImageMagick"));
			instanceMonitor.setValue("version_docker", (String) allStats.get("DockerVersion"));
			instanceMonitor.setValue("version_soffice", (String) node.get("LibreOffice"));
			instanceMonitor.setValue("statscheckdate", (String) allStats.get("CheckDate"));
			
			Map<String, Object> dockerStats = (Map<String, Object>) node.get("Docker");
			if (dockerStats != null) {
				log.info("Container found: " + dockerStats.get("Name"));
				instanceMonitor.setValue("docker-id", (String) dockerStats.get("Id"));
				instanceMonitor.setValue("docker-name", (String) dockerStats.get("Name"));
				instanceMonitor.setValue("docker-cpu", (String) dockerStats.get("Cpu"));
				instanceMonitor.setValue("docker-memory-usage", (String) dockerStats.get("MemoryUsage"));
				instanceMonitor.setValue("docker-net-down", (String) dockerStats.get("Netdown"));
				instanceMonitor.setValue("docker-net-up", (String) dockerStats.get("Netup"));
				instanceMonitor.setValue("docker-pids", (String) dockerStats.get("Pids"));
			}
			
			Map<String, Object> clusterHealth = (Map<String, Object>) node.get("clusterHealth");
			if (clusterHealth != null) {
				instanceMonitor.setValue("clusterhealth", clusterHealth.get("active_shards_percent_as_number").toString());
			}
		}
		
		// IP resolver
		String domain = (String) instanceMonitor.getValue("publicdomainname");
		if (domain != null) {
			InetAddress address = InetAddress.getByName(domain);
			instanceMonitor.setValue("publicdomainip", address.getHostAddress());
		}
		sites.saveData(instanceMonitor, null);
		
		Searcher logSearcher = inArchive.getSearcher("entermedia_instances_monitorLog");
		instanceMonitor.setId(null);
		// logSearcher.saveData(instanceMonitor, null);
		// TODO: make a log table taht actually gets vars that change
	}
	
	private Map<String, Object> httpGetRequest(String url) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost http = new HttpPost(url);
		try {
			URI uri = new URIBuilder(http.getURI()).build();
			HttpGet request = new HttpGet(uri);
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> map = mapper.readValue(EntityUtils.toString(entity), Map.class);
			return map;
		} catch (Exception e) {
			log.info(e.getMessage());
			return null;
		}
	}
	
	static String execCommand(String[] commandArr)
    {
	    String line;
	    try {
	        Process p = Runtime.getRuntime().exec(commandArr);
	        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String resp = "";
	        while ((line = stdoutReader.readLine()) != null) {
	            resp += line;
	        }
	        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	        while ((line = stderrReader.readLine()) != null) {
	            resp += line;
	        }
	        int retValue = p.waitFor();
	        if (retValue == 0) {
	        	return resp;
	        } else {
	        	return "ERROR: " + resp;
	        }
	    }
	    catch(Exception e) { 
	    	return "ERROR: " + e.toString(); 
	    }
    }

	private void enterFailover(MultiValued inReal, Data inInstance, MediaArchive inArchive) throws IOException
	{
		log.info("Entering failover...");
		sendErrorNotification(inInstance, inReal, inArchive);
		/* inReal.setValue("monitoringstatus", "error"); */
		inReal.setValue("lastcheckfail", true);
		
		if (inReal.getBoolean("isautofailover") )
		{
			AutoFailoverManager dnsManager = getAutoFailoverManager();
			Data failoverip = getMediaArchive().getData("entermedia_servers",inReal.get("failovercname"));
			Data primaryip = getMediaArchive().getData("entermedia_servers",inReal.get("primarycname"));
			String parentdomainzone = inReal.get("parentdomainzone");
			log.info(parentdomainzone + " is switching to " + failoverip);
			String publicdomainname = inReal.get("publicdomainname");
			dnsManager.updateRecord(true,parentdomainzone,publicdomainname, primaryip.get("serverip"), failoverip.get("serverip"));
		}	
	}

	private void leaveFailover(MultiValued inReal, Data inInstance, MediaArchive inArchive)
	{
		//Send email
		sendEmailResolved(inInstance, inReal, inArchive);		
		/* inReal.setValue("monitoringstatus", "ok"); */
		inReal.setValue("alerttype",new ArrayList<String>());
		inReal.setValue("lastcheckfail", false);
		
		if (inReal.getBoolean("isautofailover") )
		{
			AutoFailoverManager dnsManager = getAutoFailoverManager();
			Data failoverip = getMediaArchive().getData("entermedia_servers",inReal.get("failovercname"));
			Data primaryip = getMediaArchive().getData("entermedia_servers",inReal.get("primarycname"));
			String parentdomainzone = inReal.get("parentdomainzone");
			log.info(parentdomainzone + " is switching to " + primaryip);
			String publicdomainname = inReal.get("publicdomainname");
			dnsManager.updateRecord(false,parentdomainzone,publicdomainname, primaryip.get("serverip"), failoverip.get("serverip"));
		}	
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
	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec exec)
	{
		fieldExec = exec;
	}
	
}
