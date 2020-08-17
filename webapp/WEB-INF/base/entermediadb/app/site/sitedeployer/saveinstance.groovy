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

public void init() 
{
		
	String catalogid = "entermediadb/catalog";
    String notifyemail = "help@entermediadb.org";
	String clientemail = user.getEmail();

	
	String clientform = context.getSessionValue("clientform");
	if (clientform != null) {
		context.putSessionValue("clientform", null);
	}
	else {
		context.putPageValue("errorcode", "1");
		return;
	}

	String organizationid = context.getRequestParameter("collectionid");  //collectionid
	String instanceurl = context.getRequestParameter("organization_url");
	String instancename = context.getRequestParameter("instancename");
	String organization_type = context.getRequestParameter("organization_type");
	String timezone = context.getRequestParameter("timezone");
	String region = context.getRequestParameter("region");
	
	if (organizationid && region) {
		
		//Create Valid URL
		String selected_url = instanceurl.toLowerCase();
		context.putPageValue("selected_url", selected_url);
				
		Searcher instancesearcher = searcherManager.getSearcher(catalogid, "entermedia_instances");
		Data newinstance = instancesearcher.createNewData();
		newinstance.setValue("librarycollection", organizationid);
		newinstance.setValue("owner", user.getId());
		newinstance.setValue("instance_status", "pending");
		newinstance.setValue("name", instancename); //Needs validation?
		context.putPageValue("instancename", instancename);
		newinstance.setValue("instanceprefix", selected_url);
		newinstance.setValue("istrial", true);
		
		instancesearcher.saveData(newinstance);
		
 		HitTracker servers = mediaarchive.query("entermedia_servers").exact("allownewinstances", "true").exact("server_region", region).search();
		Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");

		Data seat = null;
		Data server = null;
		Integer nodeid = 0;
		String subnetwork = "";
		Integer maxinstances = 0;
		Integer currentinstances = 0;
		Boolean foundspace = false;
		//Check if server's seat has room
		if (servers) 
			{
			for (Iterator serverIterator = servers.iterator(); serverIterator.hasNext();)
			{
				server = serversSearcher.loadData(serverIterator.next());
				maxinstances = server.getValue("maxinstance");
				currentinstances = server.getValue("currentinstances");
				log.info("- Server: "+server.getName()+" M/C:"+maxinstances+"/"+currentinstances);
				if (currentinstances < maxinstances) {
					subnetwork = server.getValue("dockersubnet");
					nodeid = server.getValue("lastnodeid") + 1;
					foundspace = true;
					break;
				}
			 }
		
			if (!foundspace) {
				log.info("- No space on servers for trialsites");
				context.putPageValue("errorcode","2");
				
				//Send Email Notify No Space on Servers
				context.putPageValue("from", clientemail);
				context.putPageValue("subject", "No space for Trial Sites");
				sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/noseats.html");
			}
			else{
				// Call deploy script 
				try {
					ArrayList<String> command = new ArrayList<String>();
					command.add(server.sshname); //server name
					command.add(String.valueOf(subnetwork));  //server subnet
					command.add(selected_url);  //client url
					command.add(String.valueOf(nodeid));  //client nodeid				
					command.add(server.getValue("serverurl"));  // DNS
					
					Exec exec = moduleManager.getBean("exec");
					ExecResult done = exec.runExec("setupclient", command, true); //Todo: Need to move this script here?
					log.info("- Deploying Trial Site " + selected_url + " at " + server.sshname);
					
						
						String fullURL = "https://" + selected_url + "." + server.serverurl;
						
						newinstance.setValue("instanceurl", fullURL);
						newinstance.setValue("instance_status", "active");
						newinstance.setValue("instancename", selected_url);
						newinstance.setValue("instancenode", String.valueOf(nodeid));
						newinstance.setValue("istrial", true);
						newinstance.setValue("entermedia_servers", server.id);
						DateStorageUtil dateStorageUtil = DateStorageUtil.getStorageUtil();
						newinstance.setValue("datestart", new Date());
						newinstance.setValue("dateend", dateStorageUtil.addDaysToDate(new Date(), 30));
						instancesearcher.saveData(newinstance);
						
						//Update Server
						server.setValue("currentinstances", currentinstances + 1);
						if (nodeid>250) {
							server.setValue("lastnodeid", 1);
							server.setValue("dockersubnet", subnetwork + 1);
						}
						else {
							server.setValue("lastnodeid", nodeid);
						}
						serversSearcher.saveData(server);

						
						context.putPageValue("userurl",fullURL);
						
						BaseSearcher collectionsearcher = mediaarchive.getSearcher("librarycollection");
						Data collection = collectionsearcher.searchByField("id", organizationid);
						if (collection) {
							context.putPageValue("organization", collection.getValue("name"));
						}
						
						context.putPageValue("newuser", "admin");
						context.putPageValue("newpassword", "admin");
						
						//Add Site to Monitoring
						//Data monitor = addNewMonitor(newinstance);
						
		                //Send Notification to us
						context.putPageValue("from", clientemail);
						context.putPageValue("subject", "New Activation - " + fullURL);
						sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/salesnotify.html");
						
						
						//Send Email to Client
						context.putPageValue("from", notifyemail);
						context.putPageValue("subject", "Welcome to EnterMediaDB ");
						sendEmail(context.getPageMap(),clientemail,"/entermediadb/app/site/sitedeployer/email/businesswelcome.html");
				
					}
					catch(Exception e){
						 e.printStackTrace();
					}
			
			}
			}
			else {
				log.info("- No Servers Available.");
				context.putPageValue("errorcode","3");
				
			}
		
	}
}

protected Data addNewMonitor(Data instance)
{
	Searcher monitorsearcher = searcherManager.getSearcher(catalogid, "entermedia_instances_monitor");
	//TODO: set userid into client table
	Data newmonitor = monitorsearcher.createNewData();

	newmonitor.setValue("instanceid", instance.getId());
	newmonitor.setValue("name", instance.getName()); 
	newmonitor.setValue("serverid", instance.entermedia_servers);
	newmonitor.setValue("isssl", "false");
	
	newmonitor.setValue("diskmaxusage", 95); //Need to parametrize differently
	newmonitor.setValue("memmaxusage", 200);
	
	newmonitor.setValue("admin_login", "admin");
	newmonitor.setValue("admin_pass", "admin");
	
	newmonitor.setValue("monitoringenable", true);
	newmonitor.setValue("monitoringurl", instance.instanceurl);
	
	newmonitor.setValue("notifyemail", "help@entermediadb.org");  //not need it custom?
	monitorsearcher.saveData(newmonitor,null);
	return newmonitor;
}


//TODO: Make that table use the site (librarycollection)
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


