package org.entermedia.autofailover;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.Searcher;
import org.openedit.util.HttpSharedConnection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AutoFailoverManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AutoFailoverManager.class);
	
	private static final String API_ROOT_URL_PROD = "https://api.dnsimple.com/v2/" + "93771";
	private static final String API_ROOT_URL_DEV = "https://api.sandbox.dnsimple.com/v2/1114";
	
	private static final int ORIGINAL_TLL = 3600;
	private static final int FAILOVER_TLL = 60;
	
	private static final String API_TOKEN_PROD = "8XY3SwZVtQhd9iF4jTEIGZFS4viIJ2mr";
	private static final String API_TOKEN_DEV = "anaIuvWCjqlJPDYaq7Z1rTv1mBjZ8HMj";
	
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	
	private HttpSharedConnection httpconnection;
	private ObjectMapper fieldMapper;
	private Searcher dnsSearcher;

	
	public void initGeoLatencyRules()
	{
		// get UN dnsrecords_DB
		// fill failover table with priority
	}
	
	public void getList(String zone)
	{
		try
		{
			HttpGet method = null;
			
			   String url = API_ROOT_URL_PROD + "/zones/" + zone + "/records";
			   method = new HttpGet(url);
			   
			   method.setHeader("Authorization", "Bearer " + API_TOKEN_PROD);
			   method.setHeader("Content-Type", "application/json; charset=utf-8");
			   
			   HttpResponse response = getHttpConnection().getSharedClient().execute(method);
				StatusLine sl = response.getStatusLine();           
				if (sl.getStatusCode() != 200)
				{
					throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
				}
				else
				{
					ObjectMapper mapper = new ObjectMapper();
				    String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");

					JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
					JSONArray results = (JSONArray) jsonResponse.get("data");
					for (Object jsonObj : results.toArray())
					{

						JSONObject record = (JSONObject) jsonObj;
						DnsRecord dnsRecord = mapper.readValue(record.toJSONString(), DnsRecord.class);
							
						if (dnsRecord != null)
						{
							updateLocalDnsRecord(dnsRecord, null);
						}
					}
				}
		}
		catch (Exception e) {
			log.error("Can't update DNS Record", e);
		}
	}
	
	public boolean getRecord(String name)
	{
		Data dnsRecord = getMediaArchive().query("monitoredsitesdns").match("name", name).searchOne();

		if (dnsRecord != null)
		{
			try
			{
				String url = API_ROOT_URL_PROD + "/zones/" + dnsRecord.getValue("recordzone") + "/records/" + dnsRecord.getValue("recordid");
				handleRequest(HttpGet.METHOD_NAME, url, null);
			}
			catch (Exception e)
			{
				log.error("Can't get DNS Record", e);
				return false;
			}
		}
		else
		{
			log.error("Can't find any matching DNS Record on DB");
			return false;
		}
		return true;
	}

	public boolean updateRecord(String name, String content)
	{
		return updateRecord(name, content, null, null, null);
	}

	public boolean updateRecord(String name, int ttl)
	{
		return updateRecord(name, null, null, ttl, null);
	}

	public boolean updateRecord(String name, String content, int ttl)
	{
		return updateRecord(name, content, null, ttl, null);
	}
	
	public void forceLeaveFailover()
	{
		Collection<Data> records = getMediaArchive().query("monitoredsitesdns").match("isfailover", "true").search();

		if (records != null)
		{
			for (Data record : records)
			{
				updateRecord(record.get("name"), record.get("originalcontent"), (Collection<String>)record.getValue("regions"), (int)record.getValue("originalttl"), (int)record.getValue("priority"));
			}
		}
	}
	
	public boolean updateRecord(String name, String content, Collection<String> region, Integer ttl, Integer priority)
	{
		Data dnsRecord = getMediaArchive().query("monitoredsitesdns").match("name", name).searchOne();

		if (dnsRecord != null)
		{
			try
			{
				String url = API_ROOT_URL_PROD + "/zones/" + dnsRecord.getValue("recordzone") + "/records/" + dnsRecord.getValue("recordid");

				JSONObject json = new JSONObject();

				json.put("name", name);
				json.put("content", content);
				if (region != null)
				{
					json.put("regions", region);
				}
				if (ttl != null)
				{
					json.put("ttl", ttl);
				}
				if (priority != null)
				{
					json.put("priority", priority);
				}
				handleRequest(HttpPatch.METHOD_NAME, url, json);
			}
			catch (Exception e)
			{
				log.error("Can't update DNS Record", e);
				return false;
			}
		}
		else
		{
			log.error("Can't find any matching DNS Record on DB");
			return false;
		}
		return true;
	}

	public boolean createRecord(String name, String type, String zone, String content, Collection<String> region, Integer ttl, Integer priority)
	{
		try
		{
			   String url = API_ROOT_URL_PROD + "/zones/" + zone + "/records";
			   JSONObject json = new JSONObject();
			   
			   json.put("name", name);
			   json.put("type", type);
			   json.put("content", content);
			   if (region != null) 
			   {
				   json.put("regions", region);
			   }
			   if (ttl != null) 
			   {
				   json.put("ttl", ttl);
			   }
			   if (priority != null) 
			   {
				   json.put("priority", priority);
			   }

			   handleRequest(HttpPost.METHOD_NAME, url, json);
				
		}
		catch (Exception e) {
			log.error("Can't create DNS Record", e);
		}
		return true;
	}

	private void handleRequest(String method, String url, JSONObject json) throws IOException, ClientProtocolException, Exception, ParseException, JsonParseException, JsonMappingException
	{
		HttpMessage httpMethod = null;
		switch (method)
		{
		case HttpPatch.METHOD_NAME:
			httpMethod = new HttpPatch(url);
			((HttpResponse) httpMethod).setEntity(new StringEntity(json.toString(), "UTF-8"));
			break;
		case HttpPost.METHOD_NAME:
			httpMethod = new HttpPost(url);
			((HttpResponse) httpMethod).setEntity(new StringEntity(json.toString(), "UTF-8"));
			break;
		case HttpGet.METHOD_NAME:
			httpMethod = new HttpGet(url);
			break;
		default:
			break;
		}
		
		httpMethod.setHeader("Authorization", "Bearer " + API_TOKEN_PROD);
		httpMethod.setHeader("Content-Type", "application/json; charset=utf-8");

		HttpResponse response = getHttpConnection().getSharedClient().execute((HttpUriRequest) httpMethod);
		StatusLine sl = response.getStatusLine();

		if (sl.getStatusCode() != 201)
		{
			throw new Exception(method + " Request failed: status code " + sl.getStatusCode());
		}
		else
		{
			String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");

			JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
			JSONObject result = (JSONObject) jsonResponse.get("data");
			DnsRecord dnsRecord = getMapper().readValue(result.toJSONString(), DnsRecord.class);

			updateLocalDnsRecord(dnsRecord, null);
		}
	}

	private void updateLocalDnsRecord(DnsRecord inRemoteDnsRecord, Data inLocalDnsRecord)
	{
		if (inLocalDnsRecord == null)
		{
			inLocalDnsRecord = getMediaArchive().query("monitoredsitesdns").match("name", inRemoteDnsRecord.getName()).searchOne();
			if (inLocalDnsRecord == null)
			{
				inLocalDnsRecord = getDnsSearcher().createNewData();
				inLocalDnsRecord.setValue("originalcontent", inRemoteDnsRecord.getContent());
				inLocalDnsRecord.setValue("originalttl", ORIGINAL_TLL);
				inLocalDnsRecord.setValue("failoverttl", FAILOVER_TLL);
				inLocalDnsRecord.setValue("isfailover", false);
			}
		}
		inLocalDnsRecord.setValue("name", inRemoteDnsRecord.getName());
		inLocalDnsRecord.setValue("recordcontent", inRemoteDnsRecord.getContent());
		inLocalDnsRecord.setValue("recordtll", inRemoteDnsRecord.getTtl());
		inLocalDnsRecord.setValue("recordpriority", inRemoteDnsRecord.getPriority());
		inLocalDnsRecord.setValue("recordtype", inRemoteDnsRecord.getType());
		inLocalDnsRecord.setValue("monitoredsitesdnsregion", inRemoteDnsRecord.getRegions());
		inLocalDnsRecord.setValue("recordid", inRemoteDnsRecord.getId());
		inLocalDnsRecord.setValue("recordzone", inRemoteDnsRecord.getZoneId());
		getDnsSearcher().saveData(inLocalDnsRecord);
	}

	public HttpSharedConnection getHttpConnection()
	{
		if (httpconnection == null)
		{
			httpconnection = new HttpSharedConnection();
		}

		return httpconnection;
	}


	public void setHttpconnection(HttpSharedConnection inHttpconnection)
	{
		httpconnection = inHttpconnection;
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
			setMediaArchive((MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive"));
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

	public ObjectMapper getMapper()
	{
		if (fieldMapper == null)
		{
			setMapper(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
		}
		return fieldMapper;
	}

	public void setMapper(ObjectMapper inMapper)
	{
		fieldMapper = inMapper;
	}

	public Searcher getDnsSearcher()
	{
		if (dnsSearcher == null)
		{
			setDnsSearcher(getMediaArchive().getSearcher("monitoredsitesdns"));
		}
		return dnsSearcher;
	}

	public void setDnsSearcher(Searcher inDnsSearcher)
	{
		dnsSearcher = inDnsSearcher;
	}

}
