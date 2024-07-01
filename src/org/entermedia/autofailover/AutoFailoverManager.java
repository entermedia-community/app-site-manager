package org.entermedia.autofailover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AutoFailoverManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AutoFailoverManager.class);
	
	private static final String API_ROOT_URL_PROD = "https://api.dnsimple.com/v2/" + "93771";
	private static final String API_ROOT_URL_DEV = "https://api.sandbox.dnsimple.com/v2/1114";
	
	private static final int ORIGINAL_TLL = 3600;
	private static final int FAILOVER_TLL = 60;
	
//	private static final String API_TOKEN_PROD = "8XY3SwZVtQhd9iF4jTEIGZFS4viIJ2mr";
//	private static final String API_TOKEN_DEV = "anaIuvWCjqlJPDYaq7Z1rTv1mBjZ8HMj";
	
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
	

	/*

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
	*/
//	public void forceLeaveFailover()
//	{
//		Collection<Data> records = getMediaArchive().query("monitoredsitesdns").match("isfailover", "true").search();
//
//		if (records != null)
//		{
//			for (Data record : records)
//			{
//				updateRecord(record.get("name"), record.get("originalcontent"), (Collection<String>)record.getValue("regions"), (int)record.getValue("originalttl"), (int)record.getValue("priority"));
//			}
//		}
//	}
	public void updateRecord(boolean gointofailover,String parentdomain, String inPublicDomainName, String primaryip, String insecondaryip )//, Collection<String> region, Integer ttl, Integer priority)
	{
		if( parentdomain == null)
		{
			throw new OpenEditException("parentdomainzone must be set");
		}
		Long findrecordid = findRecordId(parentdomain,inPublicDomainName, primaryip, insecondaryip);
		
		String url = API_ROOT_URL_PROD + "/zones/" + parentdomain + "/records/" + findrecordid;

		JSONObject json = new JSONObject();

		//json.put("name", "ALIAS");
//				if (region != null)
//				{
//					json.put("regions", region);
//				}
//				if (ttl != null)
//				{
//					json.put("ttl", ttl);
//				}
		if(gointofailover)
		{
			json.put("content", insecondaryip);
			json.put("ttl", 120); //Failover
		}
		else
		{
			json.put("content", primaryip);
			json.put("ttl", 600);
		}

		//				if (priority != null)
//				{
//					json.put("priority", priority);
//				}
		handleRequest(url, json);
	}

	public Long findRecordId(String parentdomain, String inPublicDomain, String inPrimaryIp, String inSecondaryIp )
	{
		Collection<DnsRecord> records = getDnsRecords(parentdomain);
		String name = null;
		if(parentdomain.equals(inPublicDomain))
		{
			name = "";
		}
		else
		{
			name = inPublicDomain.substring(0,inPublicDomain.length() - parentdomain.length() -1 ); //remove dot
		}
		for (Iterator iterator = records.iterator(); iterator.hasNext();)
		{
			DnsRecord dnsRecord2 = (DnsRecord) iterator.next();
			if( "A".equals(dnsRecord2.getType()))
			{
				if( name.equals(dnsRecord2.getName() ) )
				{
					log.info("Found matching record for " + name);
					String content = dnsRecord2.getContent();
					log.info("DNS Checking " + content  + " on record id " + dnsRecord2.getId());
					if( content != null )
					{
						if( content.equals(inPrimaryIp) || content.equals(inSecondaryIp) ) 
						{
							return dnsRecord2.getId();
						}
						log.info("Could not find record id " + content);
					}
				}
			}
		}
		throw new OpenEditException("No such DNS entry found for :" + name + " using Parent Domain " + parentdomain + " with: Public domain:" + inPublicDomain);
	}
	/*
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

			   handleRequest(url, json);
				
		}
		catch (Exception e) {
			log.error("Can't create DNS Record", e);
			return false;
		}
		return true;
	}
	*/
	protected JSONObject handleRequest(String url, JSONObject json)
	{
		HttpMessage httpMethod = new HttpPatch(url);
			((HttpPatch)httpMethod).setEntity(new StringEntity(json.toString(), "UTF-8"));
		
		String apitoken = getMediaArchive().getCatalogSettingValue("site_monitor_prod_token");
		
		httpMethod.setHeader("Authorization", "Bearer " + apitoken);
		httpMethod.setHeader("Content-Type", "application/json; charset=utf-8");

		CloseableHttpResponse response = null;
		try
		{
			log.info("Setting DNS value: " + url + " with: " + json.toJSONString() );
			response = (CloseableHttpResponse)getHttpConnection().getSharedClient().execute((HttpUriRequest) httpMethod);
			StatusLine sl = response.getStatusLine();
	
			if (sl.getStatusCode() != 200)
			{
				throw new Exception(" Request failed: status code " + sl.getStatusCode());
			}
			else
			{
				String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");
	
				JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
				return jsonResponse;
//				JSONObject result = (JSONObject) jsonResponse.get("data");
//				DnsRecord dnsRecord = getMapper().readValue(result.toJSONString(), DnsRecord.class);
//				return dnsRecord;
				//updateLocalDnsRecord(dnsRecord, null);
			}
		}
		catch( Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getHttpConnection().release(response);
		}
	}

	/*
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
	*/
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

	public Collection<DnsRecord> getDnsRecords(String inDomainZone) 
	{
		try
		{
			Collection<DnsRecord> dnsrecords = new ArrayList();
			
			   String url = API_ROOT_URL_PROD + "/zones/" + inDomainZone + "/records?per_page=100&page=";

			   int maxpages = 1;
			   
			   for (int i = 0; i < maxpages; i++)
			   {
				   log.info("Loading DNS records: " + url+ (i + 1));
				   HttpGet method = new HttpGet(url + (i + 1));
				   String apitoken = getMediaArchive().getCatalogSettingValue("site_monitor_prod_token");
				   method.setHeader("Authorization", "Bearer " + apitoken);
				   method.setHeader("Content-Type", "application/json; charset=utf-8");
	
				   CloseableHttpResponse response = (CloseableHttpResponse)getHttpConnection().getSharedClient().execute(method);
				   try
				   {
						StatusLine sl = response.getStatusLine();           
						if (sl.getStatusCode() != 200)
						{
							throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
						}
						else
						{
							ObjectMapper mapper = new ObjectMapper();
						    String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");
						    //log.error("Got DNS data back " + responseJSON);
							JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
							JSONArray results = (JSONArray) jsonResponse.get("data");
							for (Object jsonObj : results.toArray())
							{
		
								JSONObject record = (JSONObject) jsonObj;
								DnsRecord dnsRecord = mapper.readValue(record.toJSONString(), DnsRecord.class);
								dnsrecords.add(dnsRecord);							
							}
							JSONObject pagination = (JSONObject) jsonResponse.get("pagination");
							if( pagination != null)
							{
								Object count = pagination.get("total_pages");
								if( count != null)
								{
									maxpages = Integer.parseInt( count.toString() );
								}
							}
						}
				   }
				   finally
				   {
					   getHttpConnection().release(response);
				   }
			   }
			return dnsrecords;
		}
		catch (Exception e) 
		{
			throw new OpenEditException("Can't update DNS Record", e);
		}
	}
}
