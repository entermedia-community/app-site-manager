package org.entermedia.sslcertificates;

import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.WebEmail;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;

public class SSLManager
{
	private static final Log log = LogFactory.getLog(SSLManager.class);
	private int DAYS_BEFORE_EXPIRATION = 10;

	private void buildEmail(Data inReal, MediaArchive inArchive)
	{
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-error.html";
		WebEmail templatemail = inArchive.createSystemEmail(inReal.get("notifyemail"), templatePage);

		if (inReal.get("sslstatus") != null)
		{
			if (inReal.get("sslstatus").compareTo("torenew") == 0)
			{
				templatemail.setSubject("[EM][" + inReal.get("name") + "][SSL][EXPIRATION] error detected");
			}
			else if (inReal.get("sslstatus").compareTo("expired") == 0)
			{
				templatemail.setSubject("[EM][" + inReal.get("name") + "][SSL][EXPIRED] error detected");
			}
			else
			{
				templatemail.setSubject("[EM][" + inReal.get("name") + "][SSL] error detected");
			}
		}

		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("monitored", inReal);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "true");

	}

	private String buildURL(Data inReal)
	{
		String dns = inReal.get("url");
		if (dns.endsWith("/"))
		{
			inReal.setProperty("url", dns.substring(0, (dns.length() - 1)));
		}
		return inReal.get("url");
	}

	public void checkExpirationDate(MediaArchive inArchive)
	{
		Searcher sites = inArchive.getSearcher("monitoredsites");
		Collection<Data> sitestomonitor = sites.query().all().search();
		Date today = DateStorageUtil.getStorageUtil().getToday(); 

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			URL url = null;

			try
			{
				SSLContext ctx = SSLContext.getInstance("TLS");

				ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());

				SSLContext.setDefault(ctx);

				if (real.get("url") != null && real.get("url").startsWith("https"))
				{
					url = new URL(buildURL(real));
					HttpsURLConnection conn = null;
					try 
					{
						conn = (HttpsURLConnection) url.openConnection();
						conn.setHostnameVerifier(new HostnameVerifier()
						{
							@Override
							public boolean verify(String arg0, SSLSession arg1)
							{
								return true;
							}
						});
						conn.connect();
					}
					catch (Exception e) {
						real.setValue("sslstatus", "error");
						throw new OpenEditException("Can't get SSL certificate from " + url);
					}
					Certificate[] certs = conn.getServerCertificates();
					
					if (certs[0] != null)
					{
						X509Certificate ssl = (X509Certificate) certs[0];
						Date expirationDate = ssl.getNotAfter();
						
						if (today.equals(expirationDate) || today.after(expirationDate))
						{
							real.setValue("sslstatus", "expired");
							throw new OpenEditException(real.get("name") + " SSL certificate has expired");
							
						}
						else if (today.after(DateStorageUtil.getStorageUtil().substractDaysToDate(expirationDate, DAYS_BEFORE_EXPIRATION)))
						{
							real.setValue("sslstatus", "torenew");
							real.setValue("expirationdate", expirationDate);
							real.setValue("daystoexpiration", DateStorageUtil.getStorageUtil().daysBetweenDates(today, expirationDate)==null?"null":DateStorageUtil.getStorageUtil().daysBetweenDates(today, expirationDate));
							throw new OpenEditException(real.get("name") + " SSL certificate is about to expire");
						}
						log.info("SSL certificate for " + url + " is expiring in on " + ssl.getNotAfter());
						real.setValue("sslstatus", "ok");
					}
					conn.disconnect();
				}
				else
				{
					real.setValue("sslstatus", "nossl");
				}
			}
			catch (Exception e)
			{
				log.error("Cant' check SSL certificate", e);
				real.setValue("isssl", true);
				if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
				{
					buildEmail(real, inArchive);
				}
			}
			sites.saveData(real, null);
		}
	}

	private static class DefaultTrustManager implements X509TrustManager
	{

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
		{
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
		{
		}

		@Override
		public X509Certificate[] getAcceptedIssuers()
		{
			return null;
		}
	}
}
