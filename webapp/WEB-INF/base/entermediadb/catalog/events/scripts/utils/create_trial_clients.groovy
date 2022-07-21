import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.data.BaseSearcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils
import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.entermediadb.websocket.chat.ChatManager
import org.openedit.Data
import org.openedit.page.Page
import org.openedit.util.PathUtilities

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



public void init() {
	
	String catalogid = "entermediadb/catalog";
	String notifyemail = "tech@entermediadb.org";
	String clientemail = user.getEmail();

	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");

	Map params = context.getSessionValue("userparams");
		
		HitTracker servers = null;
		servers = mediaarchive.query("entermedia_servers").exact("allownewinstances", true).search();
		Searcher serversSearcher = mediaarchive.getSearcher("entermedia_servers");

		Data server = null;
		Integer nodeid = 0;
		
		Integer maxinstances = 0;
		Integer currentinstances = 0;
		//Check if server's seat has room
		if (servers)
			{
			for (Iterator serverIterator = servers.iterator(); serverIterator.hasNext();)
			{
				server = serversSearcher.loadData(serverIterator.next());
				maxinstances = server.getValue("maxinstance");
				currentinstances = server.getValue("currentinstances");
				//log.info("Trial Sites - Server Instances: "+server.getName()+" Max/Current:"+maxinstances+"/"+currentinstances);
				if ((currentinstances+1) <= maxinstances) {
					try {
						//Update Server
						currentinstances = currentinstances +1 ;
						nodeid = server.getValue("lastnodeid") + 1;
	
						JSONObject jsonObject = new JSONObject();
						JSONArray jsonInstance = new JSONArray();
						JSONObject jsonInstanceObject = new JSONObject();
						
						//Next available instances
						int count = 0;
						for (int i = currentinstances; i<=maxinstances; i=i+1) {
							if (count<=20) { //always 3 more available
								jsonInstanceObject = new JSONObject();
								int nextnodeid = nodeid+count;
								jsonInstanceObject.put("containername", "t"+String.valueOf(nextnodeid));
								jsonInstance.add(jsonInstanceObject);
								count=count+1;
							}
						}
						
						jsonObject.put("available", jsonInstance);
						
						ArrayList<String> command = new ArrayList<String>();
						
						command.add("-i");
						command.add("/media/services/ansible/inventory.yml")
						command.add("/media/services/ansible/trial-create.yml");
						command.add("--extra-vars");
						command.add("server=" + server.sshname + "");
						command.add("-e");
						command.add("" + jsonObject.toJSONString() + ""); //
						
						log.info("Trial Sites - Enabling "+count+" Containers at " + server.getName());
						Exec exec = moduleManager.getBean("exec");
						ExecResult done = exec.runExec("trialsansible", command, true); //Todo: Need to move this script here?
						//ExecResult done = exec.runExec("/media/services/ansible/trialsansible.sh", command, true); //Todo: Need to move this script here?
						
					
						}
						catch(Exception e){
							context.putPageValue("status", "error");
							context.putPageValue("error", "Unexpected Error");
							 e.printStackTrace();
						}
				}
				else {
					log.info("- No space on server: "+server.getName()+" for trialsites");
					context.putPageValue("errorcode","2");
					context.putPageValue("status", "error");
					context.putPageValue("error", "No space on servers");
					//Send Email Notify No Space on Servers
				    //context.putPageValue("from", notifyemail);
				    //context.putPageValue("subject", "No space for Trial Sites");
					//sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/noseats.html");
				}
			 }
		
			
			}
			
		
}




//TODO: Make that table use the site (librarycollection)
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


