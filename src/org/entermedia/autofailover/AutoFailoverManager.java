package org.entermedia.autofailover;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.users.GroupSearcher;
import org.openedit.util.HttpSharedConnection;

public class AutoFailoverManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AutoFailoverManager.class);
	
	private static String API_ROOT_URL_PROD = "https://api.dnsimple.com/v2/" + "93771";
	private static String API_ROOT_URL_DEV = "https://api.sandbox.dnsimple.com/v2/1114";
	
	private static String API_TOKEN_PROD = "8XY3SwZVtQhd9iF4jTEIGZFS4viIJ2mr";
	private static String API_TOKEN_DEV = "anaIuvWCjqlJPDYaq7Z1rTv1mBjZ8HMj";
	
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	HttpSharedConnection httpconnection;


	
	public boolean getRecord(String name)
	{
		//Data dnsRecord = getMediaArchive().query("monitoredsites_dns").match("name", name).searchOne();

//		if (dnsRecord != null)
//		{
			try
			{
				HttpGet method = null;
				
//				   method = new HttpGet(API_TEST_ROOT_URL + "/zones/" + dnsRecord.getValue("recordzone") + "/records/" + dnsRecord.getValue("recordid"));
				   method = new HttpGet(API_ROOT_URL_PROD + "/zones/" + "openinstitute.org" + "/records/" + 944215);
				   method.setHeader("Authorization", "Bearer " + API_TOKEN_PROD);
				   method.setHeader("Accept", "application/json");
				   method.setHeader("Content-Type", "application/json");
				   
				   JSONObject json = null;
				   				
				   
				   HttpResponse response = getHttpConnection().getSharedClient().execute(method);
					StatusLine sl = response.getStatusLine();           
					if (sl.getStatusCode() != 200)
					{
						throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
					}
					else 
					{
						json = (JSONObject) new JSONParser().parse(sl.toString());

/*						JSONArray results = (JSONArray) json.get("partitions");
						for (Object partitionObj : results.toArray())
						{

							JSONObject partition = (JSONObject) partitionObj;
							DiskPartition diskPartiton = mapper.readValue(partition.toJSONString(), DiskPartition.class);

							diskPartiton.isOverloaded(inPercent);

							partitions.add(diskPartiton);

						}
*/
					}
			}
			catch (Exception e) {
				log.error("Can't update DNS Record", e);
				return false;
			}
//		}
//		else 
//		{
//			log.error("Can't find any matching DNS Record on DB");
//			return false;
//		}
		
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
	
	public boolean updateRecord(String name, String content, Collection<String> region, Integer ttl, Integer priority)
	{
		Data dnsRecord = getMediaArchive().query("monitoredsitesdns").match("name", name).searchOne();

		if (dnsRecord != null)	
		{
			try
			{
				HttpPatch method = null;
				
				   String url = API_ROOT_URL_PROD + "/zones/" + dnsRecord.getValue("recordzone") + "/records/" + dnsRecord.getValue("recordid");
				   method = new HttpPatch(url);
				   
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
				
				   StringEntity params = new StringEntity(json.toString(), "UTF-8");
				   method.setEntity(params);
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
					    String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");

						JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
						JSONObject results = (JSONObject) jsonResponse.get("data");

						Searcher dnsSearcher = getMediaArchive().getSearcher("monitoredsitesdns");
						
						dnsRecord.setValue("name", results.get("name"));
						dnsRecord.setValue("recordcontent", results.get("content"));
						dnsRecord.setValue("recordtll", results.get("ttl"));
						dnsRecord.setValue("recordpriority", results.get("priority"));
						dnsRecord.setValue("monitoredsitesdnsregion", results.get("regions"));
						dnsRecord.setValue("recordid", results.get("id"));
						
					}
			}
			catch (Exception e) {
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
			   HttpPost method = new HttpPost(API_ROOT_URL_PROD + "/zones/" + zone + "/records");
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

			   StringEntity params = new StringEntity(json.toString(), "UTF-8");
			   method.setEntity(params);
			   method.setHeader("Authorization", "Bearer " + API_TOKEN_PROD);
			   method.setHeader("Content-Type", "application/json; charset=utf-8");
		       
		       HttpResponse response = getHttpConnection().getSharedClient().execute(method);
				StatusLine sl = response.getStatusLine();           
				if (sl.getStatusCode() != 201)
				{
					throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
				}
				else
				{
				    String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");

					JSONObject jsonResponse = (JSONObject) new JSONParser().parse(responseJSON);
					JSONObject results = (JSONObject) jsonResponse.get("data");

					Searcher dnsSearcher = getMediaArchive().getSearcher("monitoredsitesdns");
					Data newDNSRecord = dnsSearcher.createNewData();
					
					newDNSRecord.setValue("name", name);
					newDNSRecord.setValue("recordzone", zone);
					newDNSRecord.setValue("recordtype", type);
					newDNSRecord.setValue("recordcontent", content);
					newDNSRecord.setValue("recordtll", ttl);
					newDNSRecord.setValue("recordpriority", priority);
					newDNSRecord.setValue("monitoredsitesdnsregion", region);
					newDNSRecord.setValue("recordid", results.get("id"));
					
					dnsSearcher.saveData(newDNSRecord);
				}
				
		}
		catch (Exception e) {
			log.error("Can't create DNS Record", e);
		}
		return true;
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

}
