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
        Searcher instancesearcher = mediaArchive.getSearcher("entermedia_instances");

        Date today = new Date();
        Collection expiredInstances = instancesearcher.query().exact("istrial", "true").exact("instance_status", "active").before("dateend", today).search();
        Collection expiredClientss = instanceSearcher.query().before("dateend", now.getTime()).search();
        log.info("Found "+ expiredClients.size() +" sites to delete.");
		log.info("Found "+ expiredClientss.size() +" sites matching.");
        expiredInstances.each{
                //Get The Client
                Data instance = instancesearcher.searchById(it.id);

                log.info("Disabling: "+instance.name+" -> "+instance.dateend);

                //Search Seat Info
                Searcher seatssearcher = mediaArchive.getSearcher("entermedia_seats");
                SearchQuery scquery = seatssearcher.createSearchQuery();
                HitTracker seats = seatssearcher.search(scquery);
                Data seat = seatssearcher.query().match("clientid", instance.id).searchOne();
                if (seat) {
                                //Get Server Info
                                Searcher servers = mediaArchive.getSearcher("entermedia_servers");
                                Data server = servers.query().exact("id", seat.trial_servers).searchOne()
                                if (server) {
                                        List<String> command = new ArrayList<String>();
                                        command.add(server.name); //server name
                                        command.add(instance.instanceurl);  //client url
                                        command.add(String.valueOf(seat.nodeid));  //client nodeid

                                        Exec exec = moduleManager.getBean("exec");
                                        ExecResult done = exec.runExec("disableclient", command);
                                        //log.info("Exec: " + done.getStandardOut());

                                        seat.setValue("clientid","");
                                        seat.setValue("seatstatus","false");
                                        seatssearcher.saveData(seat, null);

                                        //Keep the server it was installed for the record
                                        instance.setProperty("server", seat.trial_servers);
                                }
                }
                //Set Status Expired to Client
                instance.setProperty("instance_status","disabled");
                instancesearcher.saveData(instance, null);
                
                //Email Client
                context.putPageValue("client_name", instance.name);
                context.putPageValue("from", 'help@entermediadb.org');
                context.putPageValue("subject", "EnterMediaDB Instance Expired");
                sendEmail(context.getPageMap(),instance.clientemail,"/trialmanager/email/expired.html");

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
