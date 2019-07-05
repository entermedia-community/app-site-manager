package org.entermedia.speedtest;

import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.HttpRequestBuilder;

public class SpeedManager
{
	private static final Log log = LogFactory.getLog(SpeedManager.class);

	private String buildURL(Data inInstance, Data inReal, String fileURL)
	{
		if (inInstance.get("instanceurl") == null || inReal.get("catalog") == null)
		{
			throw new OpenEditException("Instance's URL or catalog missing");
		}
		String dns = inInstance.get("instanceurl");
		if (dns.endsWith("/"))
		{
			inInstance.setProperty("instanceurl", dns.substring(0, (dns.length() - 1)));
		}
		return inInstance.get("instanceurl") + "/" + inReal.get("catalog") + fileURL;
	}

	public HttpClient getClient()
	{

		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

		return httpClient;
	}

	private Long getHomepageSpeed(Data inInstance, MultiValued inReal)
	{
		HttpRequestBuilder builder = new HttpRequestBuilder();

		HttpPost postMethod = null;
		try
		{
			String fullpath = buildURL(inInstance, inReal, "/emshare/index.html");
			postMethod = new HttpPost(fullpath);

			HashMap<String, String> props = new HashMap<String, String>();

			String emkey = inReal.get("entermediadbkey");
			if (emkey == null || (emkey != null && emkey.isEmpty()))
			{
				throw new OpenEditException("Missing entermedia.key on " + inInstance.get("name"));
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

	public void checkSpeed(MediaArchive inArchive)
	{
		Searcher sites = inArchive.getSearcher("entermedia_instances_monitor");
		Collection<Data> sitestomonitor = sites.query().all().search();

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);

			if (!real.getBoolean("monitoringenable"))
			{
				inArchive.fireMediaEvent("monitoredsites", "speedcheck", real.getProperties(), null);
				continue;
			}
			
			//Get Instance Data
			Searcher instances = inArchive.getSearcher("entermedia_instances");
			Data instance = instances.query().exact("id", real.get("instanceid")).searchOne();
			if (instance == null) {
				continue;
			}

			if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareTo("ok") == 0)
			{
				try
				{
					Long elapsedTime = getHomepageSpeed(instance, real);

					if (elapsedTime != null)
					{
						real.setValue("executiontime", elapsedTime);
					}
					else
					{
						real.setValue("executiontime", "Can't retrieve stat");
					}
				}
				catch (Exception e)
				{
					log.error("Speedtest failed", e);
				}
				sites.saveData(real, null);
			}
			real.setId(null);
			inArchive.fireMediaEvent("monitoredsites", "speedcheck", real.getProperties(), null);
		}
	}

}
