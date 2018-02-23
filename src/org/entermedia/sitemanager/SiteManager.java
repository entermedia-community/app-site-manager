package org.entermedia.sitemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.email.WebEmail;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SiteManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SiteManager.class);

	private String catalogId;

	private void sendResolved(Data real, MediaArchive archive, String dates)
	{
		if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
		{
			PostMail pm = (PostMail) archive.getModuleManager().getBean("postMail");
			TemplateWebEmail email = pm.getTemplateWebEmail();
			email.setRecipientsFromCommas(real.get("notifyemail"));
			email.setSubject(real.get("name") + " error resolved");
			email.setMessage(real.get("name") + " - <a href='$monitored.url'>" + real.get("url") + "</a> - is running normally at " + dates);
			email.setFrom("monitoring@netevolved.com");
			email.send();
			real.setProperty("mailsent", "false");
		}
	}

	private boolean isOverloaded(String used, String total, int percent, boolean isDisk)
	{
		if (used == null || total == null)
			throw new OpenEditException("Can't retrieve instance's server hardware usage");
		Double dused = null;
		Double dtotal = new Double(total);
		if (isDisk)
		{
			Double dfree = new Double(used);
			dused = dtotal - dfree; 
		}
		else
		{
			dused = new Double(used);
		}

		if (((dused / dtotal) * 100) >= percent)
		{
			return true;
		}
		return false;
	}

	private Map<String, Integer> getUsageMaxByClient(Data real)
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>()
		{
			{
				Integer maxdiskusage = (Integer) real.getValue("diskmaxusage");
				Integer maxmemkusage = (Integer) real.getValue("memmaxusage");
				//CPU
				//HEAP
				
				put("MEMORY", maxmemkusage);
				put("DISK", maxdiskusage);
//				put("CPU", maxcpukusage);
//				put("HEAP", maxheapusage);
			}
		};
		return map;
	}

	private String buildURL(Data real)
	{
		String dns = real.get("url"); 
		if (dns.endsWith("/"))
		{
			real.setProperty("url", dns.substring(0, (dns.length() - 1)));
		}
		return real.get("url") + "/entermedia/services/rest/systemstatus.xml";
	}
	
	private MultiValued buildData(MultiValued real, XmlMonitoringParser parser, boolean memory, boolean cpu, boolean heap, boolean disk)
	{
		real.setProperty("heapused", parser.getHeap());
		real.setProperty("heapusedpercent", parser.getHeappercent());

		List<String> avg = null;
		
		Collection<String> average = real.getValues("loadaverage");
		if ( average != null)
		{
			avg = new ArrayList<>(average);
		}
		else
		{
			avg = new ArrayList<>();
		}
		avg.add(parser.getCpu());
		if (avg.size() > 10)
		{
			avg = avg.subList(avg.size() - 10,  avg.size() - 1);
		}
		real.setValue("loadaverage", avg);

		real.setProperty("servermemoryfree", parser.getMemoryfree());
		real.setProperty("servermemorytotal", parser.getMemorytotal());

		real.setProperty("diskfree", parser.getDiskfree());
		real.setProperty("disktotal", parser.getDisktotal());
		real.setProperty("diskavailable", parser.getDiskavailable());

		real.setProperty("ismemory", String.valueOf(memory));
		real.setProperty("iscpu", String.valueOf(cpu));
		real.setProperty("isheap", String.valueOf(heap));
		real.setProperty("isdisk", String.valueOf(disk));
		return real;
	}

	public void scan(MediaArchive archive)
	{
		log.info("starting scan");
		Collection<Data> sitestomonitor = archive.getList("monitoredsites");
		Searcher sites = archive.getSearcher("monitoredsites");

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			String url = buildURL(real);
			XmlMonitoringParser parser = null;

			Map<String, Integer> map = getUsageMaxByClient(real);
			try
			{
				boolean memory = false;
				boolean disk = false;
				boolean heap = false;
				boolean cpu = false;
				boolean reachable = false;

				URL baseurl = new URL(url);

				for (int i = 0; i < 2; i++)
				{
					try
					{
						parser = new XmlMonitoringParser(baseurl);
						reachable = true;
						break;
					}
					catch (Exception e)
					{
					}
					TimeUnit.SECONDS.sleep(10);
				}				
				if (!reachable)
				{
					real.setValue("isreachable", false);
					throw new OpenEditException("Server unreachable");
				}

				if (parser.getStat() != null && parser.getStat().compareToIgnoreCase("ok") > 0)
				{
					throw new OpenEditException("Server returns invalid status");
				}				

				memory = isOverloaded(parser.getMemoryfree(), parser.getMemorytotal(), map.get("MEMORY"), false);
				disk = isOverloaded(parser.getDiskavailable(), parser.getDisktotal(), map.get("DISK"), true);
				if (parser.getHeappercent() != null)
				{
					heap = new Double(parser.getHeappercent()) >= 90 ? true : false;
				}
				if (parser.getCpu() != null)
				{
					cpu = new Double(parser.getCpu()) >= 90 ? true : false;

				}

				real = buildData(real, parser, memory, cpu, heap, disk);
				
				if (memory || heap || cpu || disk)
				{
					throw new OpenEditException("Hardware overload");
				}

				if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareToIgnoreCase("error") == 0)
				{
					sendResolved(real, archive, dates);
				}
				real.setProperty("monitoringstatus", "ok");

			}
			catch (Exception e)
			{
				log.error("Error checking " + real.get("name"), e);
				real.setProperty("monitoringstatus", "error");

				if (!Boolean.parseBoolean(real.get("mailsent")))
				{
					if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
					{
						buildEmail(real, archive);
					}
				}
			}
			real.setProperty("lastchecked", dates);
			sites.saveData(real, null);
		}
	}

	private void buildEmail(Data real, MediaArchive archive)
	{
		String templatePage = "/" + archive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-error.html";
		WebEmail templatemail = archive.createSystemEmail(real.get("notifyemail"), templatePage);
		
		templatemail.setSubject("[EM][" + real.get("name") + "] error detected");
		Map objects = new HashMap();
		objects.put("monitored", real);
		templatemail.send(objects);
		real.setProperty("mailsent", "true");

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

}
