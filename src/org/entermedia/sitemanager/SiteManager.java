package org.entermedia.sitemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.WebEmail;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;

import java.io.File;
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

	private void sendResolved(Data inReal, MediaArchive inArchive, String inDates)
	{
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-resolve.html";
		WebEmail templatemail = inArchive.createSystemEmail(inReal.get("notifyemail"), templatePage);

		templatemail.setSubject("[EM][" + inReal.get("name") + "] error resolved");
		Map objects = new HashMap();
		objects.put("monitored", inReal);
		objects.put("dates", inDates);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "false");
	}

	private void buildEmail(Data inReal, MediaArchive inArchive)
	{
		String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/monitoring-error.html";
		WebEmail templatemail = inArchive.createSystemEmail(inReal.get("notifyemail"), templatePage);
		
		templatemail.setSubject("[EM][" + inReal.get("name") + "] error detected");
		Map objects = new HashMap();
		objects.put("monitored", inReal);
		templatemail.send(objects);
		inReal.setProperty("mailsent", "true");

	}

	private boolean isOverloaded(String inUsed, String inTotal, int inPercent, boolean inIsDisk)
	{
		if (inUsed == null || inTotal == null)
			throw new OpenEditException("Can't retrieve instance's server hardware usage");
		Double dused = null;
		Double dtotal = new Double(inTotal);
		if (inIsDisk)
		{
			Double dfree = new Double(inUsed);
			dused = dtotal - dfree; 
		}
		else
		{
			dused = new Double(inUsed);
		}

		if (((dused / dtotal) * 100) >= inPercent)
		{
			return true;
		}
		return false;
	}

	private Map<String, Integer> getUsageMaxByClient(Data inReal)
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
			}
		};
		return map;
	}


	private MultiValued buildData(MultiValued inReal, XmlMonitoringParser inParser, boolean inMemory, boolean inCpu, boolean inHeap, Boolean inDisk)
	{
		inReal.setProperty("heapused", inParser.getHeap());
		inReal.setProperty("heapusedpercent", inParser.getHeappercent());

		List<String> avg = null;
		
		Collection<String> average = inReal.getValues("loadaverage");
		if ( average != null)
		{
			avg = new ArrayList<>(average);
		}
		else
		{
			avg = new ArrayList<>();
		}
		avg.add(inParser.getCpu());
		if (avg.size() > 10)
		{
			avg = avg.subList(avg.size() - 10,  avg.size() - 1);
		}
		inReal.setValue("loadaverage", avg);
		inReal.setValue("isreachable", true);

		inReal.setProperty("servermemoryfree", inParser.getMemoryfree());
		inReal.setProperty("servermemorytotal", inParser.getMemorytotal());

		inReal.setProperty("ismemory", String.valueOf(inMemory));
		inReal.setProperty("iscpu", String.valueOf(inCpu));
		inReal.setProperty("isheap", String.valueOf(inHeap));
		if (inDisk != null)
		{
			inReal.setProperty("isdisk", String.valueOf(inDisk));
		}
		return inReal;
	}
	
	private String buildURL(Data inReal)
	{
		String dns = inReal.get("url"); 
		if (dns.endsWith("/"))
		{
			inReal.setProperty("url", dns.substring(0, (dns.length() - 1)));
		}
		return inReal.get("url") + "/entermedia/services/rest/systemstatus.xml";
	}
	
	private Boolean checkDisksOverload(MultiValued inReal, int inPercent, ArrayList<DiskPartition> inPartitions)
	{
		boolean isOnePartitionOverloaded = false;

		if (inReal.get("diskpartitions") == null)
		{
			return null;
		}
		
		String[] fileNames = inReal.get("diskpartitions").split(",");

		for (String it : fileNames)
		{
			File partition = new File(it);
			
	        Long totalCapacity = (long)(partition.getTotalSpace() / 10000000.00);
	        Long freePartitionSpace = (long)(partition.getFreeSpace() / 10000000.00);
	        Long usablePartitionSpace = (long)(partition.getUsableSpace() / 10000000.00);
			
	        boolean isOverloaded = isOverloaded(freePartitionSpace.toString(), totalCapacity.toString(), inPercent, true);
	        inPartitions.add(new DiskPartition(it, totalCapacity, freePartitionSpace, usablePartitionSpace, isOverloaded));
	        if (!isOnePartitionOverloaded && isOverloaded == true)
	        {
	        	isOnePartitionOverloaded = true;
	        }
		}
		return isOnePartitionOverloaded;
	}
	
	public void scan(MediaArchive inArchive)
	{
		log.info("starting scan");
		Collection<Data> sitestomonitor = inArchive.getList("monitoredsites");
		Searcher sites = inArchive.getSearcher("monitoredsites");

		for (Data it : sitestomonitor)
		{
			MultiValued real = (MultiValued) sites.loadData(it);
			String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			String url = buildURL(real);
			XmlMonitoringParser parser = null;

			Map<String, Integer> map = getUsageMaxByClient(real);
			ArrayList<DiskPartition> partitions = new ArrayList<DiskPartition>();
			try
			{
				Boolean disk = false;
				boolean memory = false;
				boolean heap = false;
				boolean cpu = false;
				boolean reachable = false;

				URL baseurl = new URL(url);

				for (int i = 0; i < 3; i++)
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
					TimeUnit.SECONDS.sleep(5);
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

				//TODO Calculate mem usage average over the past X Hour/minutes
				memory = isOverloaded(parser.getMemoryfree(), parser.getMemorytotal(), map.get("MEMORY"), false);
				
				
				disk = checkDisksOverload(real, map.get("DISK"), partitions);
				
				if (disk == null)
				{
					real.setValue("nodisk", true);
				}
				else
				{
					real.setValue("nodisk", false);
					real.setValue("partitions", partitions);
				}
				
				if (parser.getHeappercent() != null)
				{
					heap = new Double(parser.getHeappercent()) >= 90 ? true : false;
				}
				if (parser.getCpu() != null)
				{
					cpu = new Double(parser.getCpu()) >= 90 ? true : false;

				}

				real = buildData(real, parser, memory, cpu, heap, disk);
				
				if (memory || heap || cpu || (disk == null || disk))
				{
					throw new OpenEditException("Hardware overload");
				}

				if (real.get("monitoringstatus") != null && real.get("monitoringstatus").compareToIgnoreCase("error") == 0)
				{
					if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
					{
						sendResolved(real, inArchive, dates);
					}
				}
				real.setProperty("monitoringstatus", "ok");

			}
			catch (Exception e)
			{
				log.error("Error checking " + real.get("name"), e);
				e.printStackTrace();
				real.setProperty("monitoringstatus", "error");

				if (!Boolean.parseBoolean(real.get("mailsent")))
				{
					if (real.get("notifyemail") != null && !real.get("notifyemail").isEmpty())
					{
						buildEmail(real, inArchive);
					}
				}
			}
			real.setProperty("lastchecked", dates);
			sites.saveData(real, null);
		}
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
