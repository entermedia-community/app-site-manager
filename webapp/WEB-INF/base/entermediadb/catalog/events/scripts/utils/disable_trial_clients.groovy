package utils;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init() {
	//String catalogid = "sitemanager/catalog";

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");

	//Search Clients with End Date = Today
	Searcher instanceSearcher = mediaArchive.getSearcher("entermedia_instances");

	Date today = new Date();
	Collection expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "active").before("dateend", today).search();

	if (!expiredInstances.size())
	{
		expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "todisable").search();
	}

	log.info("Found "+ expiredInstances.size() +" sites to disable.");

	for (Iterator instanceIterator = expiredInstances.iterator(); instanceIterator.hasNext();)
	{
		//Get The Instance
		Data instance = instanceSearcher.loadData(instanceIterator.next());
		if (instance.lastlogin == null || instance.instance_status == "todisable") {
			log.info("Disabling: "+instance.instancename+" -> "+instance.dateend);
			//Get Server Info
			Searcher serversSearcher = mediaArchive.getSearcher("entermedia_servers");
			Data server = serversSearcher.query().exact("id", instance.entermedia_servers).searchOne();
			if (server)
			{
				JSONObject jsonObject = new JSONObject();
				JSONArray jsonInstance = new JSONArray();
				JSONObject jsonInstanceObject = new JSONObject();
	
				jsonInstanceObject.put("subdomain", instance.instancename);
				jsonInstanceObject.put("containername", "t"+instance.instancenode);
				jsonInstance.add(jsonInstanceObject);
	
				jsonObject.put("disabled", jsonInstance);
	
				ArrayList<String> command = new ArrayList<String>();
	
				command.add("-i");
				command.add("/media/services/ansiblea/inventory.yml")
				command.add("/media/services/ansible/trial.yml");
				command.add("--extra-vars");
				command.add("server=" + server.sshname + "");
				command.add("-e");
				command.add("" + jsonObject.toJSONString() + "");
	
				Exec exec = moduleManager.getBean("exec");
				ExecResult done = exec.runExec("trialsansible", command, true);
	
				if(instance.getValue("instance_status") == 'active')
				{
					server.setValue("currentinstances", server.getValue("currentinstances") - 1);
				}
				serversSearcher.saveData(server);
			}
	
	
			//Set Status Expired to Client
			instance.setProperty("instance_status","disabled");
			log.info("updated instance");
	
			//Disable Monitoring
			Searcher monitorsearcher = mediaArchive.getSearcher("entermedia_instances_monitor");
			Data instancemonitor = monitorsearcher.query().match("instanceid", instance.id).searchOne();
			if (instancemonitor)
			{
				instancemonitor.setValue("monitoringenable","false");
				monitorsearcher.saveData(instancemonitor, null);
			}
		}

		//Email Client
		//		if (instance.owner)
		//		{
		//
		//			context.putPageValue("from", 'help@entermediadb.org');
		//			context.putPageValue("subject", "EnterMediaDB Instance Expired");
		//			Searcher usersearcher = mediaArchive.getSearcher("user");
		//			Data owner = usersearcher.query().exact("id", instance.owner).searchOne();
		//			if (owner.email)
		//			{
		//				context.putPageValue("client_name", user.name);
		//				sendEmail(context.getPageMap(), owner.email, "/entermediadb/app/site/sitedeployer/email/expired.html");
		//			}
		//		}
	}
}

//protected void sendEmail(Map pageValues, String email, String templatePage){
//	//send e-mail
//	//Page template = getPageManager().getPage(templatePage);
//	RequestUtils rutil = moduleManager.getBean("requestUtils");
//	BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage,null, null);
//
//	newcontext.putPageValues(pageValues);
//
//	PostMail mail = (PostMail)moduleManager.getBean( "postMail");
//	TemplateWebEmail mailer = mail.getTemplateWebEmail();
//	mailer.loadSettings(newcontext);
//	mailer.setMailTemplatePath(templatePage);
//	mailer.setRecipientsFromCommas(email);
//	//mailer.setMessage(inOrder.get("sharenote"));
//	//mailer.setWebPageContext(context);
//	mailer.send();
//	log.info("email sent to ${email}");
//}

init();
