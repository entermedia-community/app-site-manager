package utils;

import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.entermediadb.asset.MediaArchive
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil

import org.openedit.BaseWebPageRequest
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init()
{
        //String catalogid = "sitemanager/catalog";

        MediaArchive mediaArchive = context.getPageValue("mediaarchive");

        //Search Clients with End Date = Today
        Searcher instanceSearcher = mediaArchive.getSearcher("entermedia_instances");

        Date today = new Date();
        Collection expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "active").before("dateend", today).search();
        
		
		if (!expiredInstances.size()) {
			expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "todisable").search();
		}
		
		log.info("Found "+ expiredInstances.size() +" sites to disable.");
		
		for (Iterator instanceIterator = expiredInstances.iterator(); instanceIterator.hasNext();)
		{
            //Get The Instance
			Data instance = instanceSearcher.loadData(instanceIterator.next());
            log.info("Disabling: "+instance.name+" -> "+instance.dateend);
            //Get Server Info
                Searcher servers = mediaArchive.getSearcher("entermedia_servers");
                Data server = servers.query().exact("id", instance.entermedia_servers).searchOne();
                if (server) {
                        List<String> command = new ArrayList<String>();
                        command.add(server.name); //server name
                        command.add(instance.instanceprefix);  //client url
                        command.add(String.valueOf(seat.nodeid));  //client nodeid

                        Exec exec = moduleManager.getBean("exec");
                        ExecResult done = exec.runExec("trialdisable", command);
                        log.info("Exec: " + done.getStandardOut());
                }
            //Set Status Expired to Client
            instance.setProperty("instance_status","disabled");
            instanceSearcher.saveData(instance, null);
			
			//Disable Monitoring
			Searcher monitorsearcher = mediaArchive.getSearcher("entermedia_instances_monitor");
			Data instancemonitor = monitorsearcher.query().match("instanceid", instance.id).searchOne();
			if (instancemonitor) {
				instancemonitor.setValue("monitoringenable","false");
				monitorsearcher.saveData(instancemonitor, null);
			}
            
            //Email Client
			if (instance.owner)
			{
	            
	            context.putPageValue("from", 'help@entermediadb.org');
	            context.putPageValue("subject", "EnterMediaDB Instance Expired");
				Searcher usersearcher = mediaArchive.getSearcher("user");
				Data owner = usersearcher.query().exact("id", instance.owner).searchOne();
				if (owner.email) 
				{
					context.putPageValue("client_name", user.name);
					sendEmail(context.getPageMap(), owner.email, "/entermediadb/app/site/sitedeployer/email/expired.html");
				}
			}
        }
}

protected void sendEmail(Map pageValues, String email, String templatePage){
	//send e-mail
	//Page template = getPageManager().getPage(templatePage);
	RequestUtils rutil = moduleManager.getBean("requestUtils");
	BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage,null, null);

	newcontext.putPageValues(pageValues);

	PostMail mail = (PostMail)moduleManager.getBean( "postMail");
	TemplateWebEmail mailer = mail.getTemplateWebEmail();
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	//mailer.setMessage(inOrder.get("sharenote"));
	//mailer.setWebPageContext(context);
	mailer.send();
	log.info("email sent to ${email}");
}

init();
