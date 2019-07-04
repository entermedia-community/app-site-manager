import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
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
		context.putPageValue("errormsg", "Invalid Submission. Please  <a href='./createsite.html'>try again</a>.");
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
		
		//Search Server by Region
		log.info("- Checking region " + region);
		
 		HitTracker servers = mediaarchive.query("entermedia_servers").match("server_region", region).search();
		Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");

		Data seat = null;
		Data server = null;
		//Check if server's seat has room
		if (servers) 
			{
			for (Iterator serverIterator = servers.iterator(); serverIterator.hasNext();)
			{
				server = serversSearcher.loadData(serverIterator.next());
				log.info("- Checking server " + server.getName());
				HitTracker seats = mediaarchive.query("entermedia_seats").match("seatstatus", "true").match("entermedia_servers", server.id).search();
				if ( seats.size() < Integer.parseInt(server.maxinstance) )
				{
					seat = mediaarchive.query("entermedia_seats").match("seatstatus", "false").match("entermedia_servers", server.id).searchOne();
					break;
				}
			 }
		
			if (seat == null) {
				log.info("No seats available. Max: "+server.maxinstance);
				context.putPageValue("errormsg","<h1>Ups!</h1><p>No Demo sites available for now. Please contact <a href='mailto:help@entermediadb.org'>EnterMedia Support Team</a>.</p>");
				
				//Send Email Notify No Seats
				context.putPageValue("from", clientemail);
				context.putPageValue("subject", "No Seats Available for Trial Sites");
				sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/noseats.html");
			}
			else{
				log.info("Seat found: " + seat);
				// Call deploy script 
				try {
					List<String> command = new ArrayList<String>();
					command.add(server.sshname); //server name
					command.add(server.dockersubnet);  //server subnet
					command.add(selected_url);  //client url
					command.add(String.valueOf(seat.nodeid));  //client nodeid				
					command.add(server.getValue("serverurl"));  // DNS
					
					Exec exec = moduleManager.getBean("exec");
					ExecResult done = exec.runExec("setupclient", command); //Todo: Need to move this script here?
					log.info("Exec: " + done.getStandardOut());
					
						
						String fullURL = "https://" + selected_url + "." + server.serverurl;
						
						newinstance.setValue("instanceurl", fullURL);
						newinstance.setValue("instance_status", "active");
						newinstance.setValue("istrial", true);
						newinstance.setValue("entermedia_servers", server.id);
						DateStorageUtil dateStorageUtil = DateStorageUtil.getStorageUtil();
						newinstance.setValue("datestart", new Date());
						newinstance.setValue("dateend", dateStorageUtil.addDaysToDate(new Date(), 30));
						instancesearcher.saveData(newinstance);
						
						//Assign client to seat
						seat.setValue("instanceid", newinstance.getId());
						seat.setValue("seatstatus", "true");
						mediaarchive.saveData("entermedia_seats", seat);

						context.putPageValue("userurl",fullURL);
						//context.putPageValue("client_name", organization);
						context.putPageValue("newuser", "admin");
						context.putPageValue("newpassword", "admin");
						
						//Add Site to Monitoring
						Data monitor = addNewMonitor(newinstance);
						
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
				log.info("No servers available.");
				context.putPageValue("errormsg","<h1>Ups!</h1><p>No Demo sites available for now. Please contact <a href='mailto:help@entermediadb.org'>EnterMedia Support Team</a>.</p>");
				
			}
		
	}
}

protected Data addNewMonitor(Data instance)
{
	Searcher monitorsearcher = searcherManager.getSearcher(catalogid, "entermedia_instances_monitor");
	//TODO: set userid into client table
	Data newmonitor = monitorsearcher.createNewData();

	newmonitor.setValue("instanceid", instance.getId());
	newmonitor.setValue("serverid", instance.entermedia_servers);
	newmonitor.setValue("isssl", "false");
	
	newmonitor.setValue("diskmaxusage", 95); //Need to parametrize differently
	newmonitor.setValue("memmaxusage", 200);
	
	newmonitor.setValue("admin_login", "admin");
	newmonitor.setValue("admin_pass", "admin");
	
	newmonitor.setValue("monitoringenable", true);
	
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