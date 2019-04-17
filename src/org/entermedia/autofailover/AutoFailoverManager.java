package org.entermedia.autofailover;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.update.Downloader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;

public class AutoFailoverManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AutoFailoverManager.class);
	
	private static String API_ROOT_URL = "https://api.dnsimple.com/v2/" + "accountid";
	private static String API_TEST_ROOT_URL = "https://api.sandbox.dnsimple.com/v2/" + "accountid";
	
	private static String API_TOKEN = "anaIuvWCjqlJPDYaq7Z1rTv1mBjZ8HMj";
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;

	
	public boolean getRecord(String type, String value, String Region)
	{
		
		Downloader downloader = new Downloader();


		return true;
	}

	public boolean updateRecord(String name, String value, Collection<String> region, int ttl, int priority)
	{
		
		Data dnsRecord = getMediaArchive().query("monitoredsites_dns").match("name", name).searchOne();

		if (dnsRecord != null)
		{
			FileOutputStream out = null;
			InputStream in  = null;
			HttpPatch method = null;
			try
			{

				  RequestConfig globalConfig = RequestConfig.custom()
			                .setCookieSpec(CookieSpecs.DEFAULT)
			                .build();
			        HttpClient client = HttpClients.custom()
			                .setDefaultRequestConfig(globalConfig)
			                .build();

				   method = new HttpPatch(API_ROOT_URL + "/zones/" + dnsRecord.getValue("record_zone") + "/records/" + dnsRecord.getValue("record_id"));
				   method.setHeader("Authorization", "Bearer " + API_TOKEN);
				   method.setHeader("Accept", "application/json");
				   method.setHeader("Content-Type", "application/json");
				   
				   JSONObject json = new JSONObject();
				   
				   json.put("name", name);
				   json.put("value", name);
				   //json.put("region", region);
				   json.put("ttl", ttl);
				   json.put("priority", priority);

				   StringEntity params =new StringEntity(json.toString());
				   method.setEntity(params);
			     //  HttpRequestBuilder builder = new HttpRequestBuilder();

			      // method.setEntity(builder.build());
			       
			       HttpResponse response2 = client.execute(method);
					StatusLine sl = response2.getStatusLine();           
					if (sl.getStatusCode() != 200)
					{
						throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
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

	public boolean createRecord(String name, String zone, String value, Collection<String> region, int ttl, int priority)
	{
		HttpPatch method = null;
		try
		{

			  RequestConfig globalConfig = RequestConfig.custom()
		                .setCookieSpec(CookieSpecs.DEFAULT)
		                .build();
		        HttpClient client = HttpClients.custom()
		                .setDefaultRequestConfig(globalConfig)
		                .build();

			   method = new HttpPatch(API_ROOT_URL + "/zones/" + zone + "/records");
			   method.setHeader("Authorization", "Bearer " + API_TOKEN);
			   method.setHeader("Accept", "application/json");
			   method.setHeader("Content-Type", "application/json");
			   
			   JSONObject json = new JSONObject();
			   
			   json.put("name", name);
			   json.put("value", name);
			   //json.put("region", region);
			   json.put("ttl", ttl);
			   json.put("priority", priority);

			   StringEntity params =new StringEntity(json.toString());
			   method.setEntity(params);
		       
		       HttpResponse response2 = client.execute(method);
				StatusLine sl = response2.getStatusLine();           
				if (sl.getStatusCode() != 200)
				{
					throw new Exception( method  + " Request failed: status code " + sl.getStatusCode());
				}
		}
		catch (Exception e) {
			log.error("Can't update DNS Record", e);
		}
		return true;
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
