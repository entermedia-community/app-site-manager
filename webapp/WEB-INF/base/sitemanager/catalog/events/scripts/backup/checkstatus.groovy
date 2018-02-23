package backup

import java.util.logging.Logger
import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.entermediadb.modules.update.Downloader
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil
import org.openedit.page.Page
import org.openedit.WebPageRequest
import java.math.*
import java.util.Map

private void sendResolved(Data real, MediaArchive archive, String dates) {
	if(real.notifyemail){
		PostMail pm = archive.getModuleManager().getBean("postMail");
		TemplateWebEmail email = pm.getTemplateWebEmail();
		email.setRecipientsFromCommas(real.get("notifyemail"));
		email.setSubject("${real.name} error resolved");
		email.setMessage("${real.name} is running normally at ${dates}");
		email.setFrom("monitoring@netevolved.com");
		email.send();
	}
}

private boolean isOverloaded(Double free, Double total, int percent) {
		double result = (free/total)*100;
 
		if (result >= percent)
			return true;
		return false;
}

private Map<String, Integer> getUsageMaxByClient() {
		Map<String, Integer> map = new HashMap<Integer, String>() {
	        {
				int maxdiskusage = real.getValue("maxdiskusage");
				int maxmemkusage = real.getValue("maxmemusage");

	            put("MEMORY", maxmemkusage);
	            put("DISK", maxdiskusage);
				log.info("max mem: " + maxmemkusage);
				log.info("max disk: " + maxdiskusage);
			}
		};
		return map;
}

public void init(){
	log.info("starting scan");
	MediaArchive archive = context.getPageValue("mediaarchive");
	HitTracker sitestomonitor = archive.getList("monitoredsites");
	Searcher sites = archive.getSearcher("monitoredsites");
	
	sitestomonitor.each{
		Data real= archive.getData("monitoredsites", it.id);
		String dates = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		String baseurl = "${real.url}/entermedia/services/rest/systemstatus.xml";
		
		Map<String, Integer> map = getUsageMaxByClient();
		try {
			boolean memory = false;
			boolean disk = false;
			boolean heap = false;
			boolean cpu = false;
			
			Downloader dl = new Downloader();
			String download = dl.downloadToString(baseurl);
			def sp = new XmlSlurper().parseText(download);

			memory = isOverloaded(sp.servermemoryfree.toDouble(), sp.servermemorytotal.toDouble(), map["MEM"]);
			disk= isOverloaded(sp.diskfree.toDouble(), sp.disktotal.toDouble(), map["DISK"]);
			heap = sp.heapusedpercent.toDouble()>=90?true:false;
			cpu = sp.loadaverage.toDouble()>=90?true:false;
			
			
			String status = sp.@stat;
			if(status != "ok"){
				throw new OpenEditException("Server unreachable");
			}
			
			if(real.monitoringstatus == "error"){
				sendResolved(real, archive, dates);
			}
			
			real.setProperty("mailsent", "false");
			real.setProperty("monitoringstatus", "ok");
			
		
			real.setProperty("heapused",  sp.headused.text());
			real.setProperty("heapusedpercent",  sp.heapusedpercent.text());
			real.setProperty("loadaverage",  sp.loadaverage.text());
			real.setProperty("servermemoryfree",  sp.servermemoryfree.text());
			real.setProperty("servermemorytotal",  sp.servermemorytotal.text());
			
			
			String free = sp.diskfree.text();
			real.setProperty("diskfree",  free);
			real.setProperty("disktotal",  sp.disktotal.text());
			real.setProperty("diskavailable",  sp.diskavailable.text());
			
			real.setProperty("ismemory",  memory);
			real.setProperty("iscpu",  cpu);
			real.setProperty("isheap",  heap);
			real.setProperty("isdisk",  true);
			
			
			// check bool values and throw exception if overload then check all overloads in catch
			if (memory || heap || cpu || disk) {
				throw new OpenEditException("Hardware overload");
			}
			
			
		} catch (Exception e){
			log.error("Error checking ${real.name} ", e);
			real.setProperty("monitoringstatus", "error");
			
			if(!Boolean.parseBoolean(real.get("mailsent"))){
				if(real.notifyemail){
					PostMail pm = archive.getModuleManager().getBean("postMail");
					TemplateWebEmail email = pm.getTemplateWebEmail();
					
					
					String templatePage = "/${archive.getCatalogSettingValue('events_notify_app')}/theme/emails/monitoring-error.html";
					Page template = pageManager.getPage(templatePage);
					context.putPageValue("monitored", real);
					WebPageRequest newcontext = context.copy(template);
				
					email.loadSettings(newcontext);
					email.setMailTemplatePath(templatePage);
				
					email.setRecipientsFromCommas(real.get("notifyemail"));
					email.setSubject("${real.name} error detected");
					//email.setMessage("${real.name} has entered an error state - current time is ${dates}");
					email.setFrom("monitoring@netevolved.com");
					email.send();
					real.setProperty("mailsent", "true");
				}
			}
		}
		real.setProperty("lastchecked", dates);
		sites.saveData(real, null);
	}
}
init();

