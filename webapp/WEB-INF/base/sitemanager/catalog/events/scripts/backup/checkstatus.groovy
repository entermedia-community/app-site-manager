package backup

import java.util.logging.Logger;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.entermediadb.modules.update.Downloader
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil

public void init(){

	log.info("starting scan");
	MediaArchive archive = context.getPageValue("mediaarchive");
	HitTracker sitestomonitor = archive.getList("monitoredsites");
	Searcher sites = archive.getSearcher("monitoredsites");
	sitestomonitor.each{
		Data real= archive.getData("monitoredsites", it.id);
		String dates = 		DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		String baseurl = "${real.url}/entermedia/services/rest/systemstatus.xml";
		try{
			Downloader dl = new Downloader();
			String download = dl.downloadToString(baseurl);
			def sp = new XmlSlurper().parseText(download);

			String status = sp.@stat;
			if(status != "ok"){
				throw new OpenEditException("Error!");
			}
			if(real.monitoringstatus == "error"){
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
			real.setProperty("mailsent", "false");
			real.setProperty("monitoringstatus", "ok");
			
			
//			<heapused>$stats.getJvm().getMem().getHeapUsed()</heapused>
//			<heapusedpercent> $stats.getJvm().getMem().getHeapUsedPercent()</heapusedpercent>
//			<loadaverage>$stats.getOs().getLoadAverage()</loadaverage>
//			<servermemory>$stats.getOs().getMem().getFree()</servermemory>
		
		
			real.setProperty("heapused",  sp.headused.text());
			real.setProperty("heapusedpercent",  sp.heapusedpercent.text());
			real.setProperty("loadaverage",  sp.loadaverage.text());
			real.setProperty("servermemory",  sp.servermemory.text());
			
			real.setProperty("diskfree",  sp.diskfree.text());
			real.setProperty("disktotal",  sp.disktotal.text());
			real.setProperty("diskavailable",  sp.diskavailable.text());
			
			
			
			
			
		} catch (Exception e){
			log.error("Error checking ${real.name} ", e)
			real.setProperty("monitoringstatus", "error");
			if(!Boolean.parseBoolean(real.get("mailsent"))){
				if(real.notifyemail){
					PostMail pm = archive.getModuleManager().getBean("postMail");
					TemplateWebEmail email = pm.getTemplateWebEmail();
					email.setRecipientsFromCommas(real.get("notifyemail"));
					email.setSubject("${real.name} error detected");
					email.setMessage("${real.name} has entered an error state - current time is ${dates}");
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

