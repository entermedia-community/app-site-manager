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
    String notifyemail = "cristobal@entermediadb.org";
	String clientemail = user.getEmail();


/*	
	String clientform = context.getSessionValue("clientform");
	if (clientform != null) {
		context.putSessionValue("clientform", null);
	}
	else {
		context.putPageValue("errormsg", "Invalid Submission. Please  <a href='./createsite.html'>try again</a>.");
		return;
	}
*/
	String organizationid = context.getRequestParameter("collectionid");  //collectionid
	String instanceurl = context.getRequestParameter("organization_url");
	String instancename = context.getRequestParameter("instancename");
	String organization_type = context.getRequestParameter("organization_type");
	String timezone = context.getRequestParameter("timezone");
	String region = context.getRequestParameter("region");
	
	if (organizationid && region) {

/*		
		Searcher clientsearcher = searcherManager.getSearcher(catalogid, "entermedia_clients");
		Data newclient = clientsearcher.createNewData();
		newclient.setValue("userid", user.getId());
		newclient.setValue("name", organization);
		newclient.setValue("clientemail",email);
		newclient.setValue("clientcategory", organization_type);
		newclient.setValue("timezone", timezone);

		clientsearcher.saveData(newclient,null);
*/
		
		//Create Valid URL
		String selected_url = instanceurl.toLowerCase();
		context.putPageValue("selected_url", selected_url);
		//----context.putPageValue("organization", organization);
				
		Searcher instancesearcher = searcherManager.getSearcher(catalogid, "entermedia_instances");
		Data newinstance = instancesearcher.createNewData();
		newinstance.setValue("librarycollection", organizationid);
		newinstance.setValue("instance_status", "pending");
		newinstance.setValue("name", instancename); //Needs validation?
		newinstance.setValue("instanceurl", selected_url);
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
					if (done.getStandardOut() != null) {
						
						String fullURL = selected_url + "." + server.serverurl;
						
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
						
						
						
						/*
						Data client = mediaarchive.query("client").match("name", organization).searchOne();
						Data group = null;
						if ( client == null )
						{
							log.info("Client not found, creating new client and group");
							client = addNewClient();
							group = addNewGroup();
							Searcher userSearcher = searcherManager.getSearcher(catalogid, "user");
							
							Collection groups = (Collection)user.getValues("groups");
							if (groups == null)
							{
								groups = new ArrayList<Data>();
							}
				
							groups.add(group.getId());
							user.setValue("groups", groups);
							userSearcher.saveData(user, null);
						}
		                else {
		                    //Account exists, send error message to let them know
							group = mediaarchive.query("group").match("name", organization).searchOne();
		                }
					
		
		                //Add Site to Monitoring
						Data monitor = addNewMonitor(server, "http://" + fullURL, client.id);
		
						//Add New Collection
						//addNewCollection(fullURL, newclient.id, monitor.id, group.id);
		
						
										
		
						
						Collection instances = (Collection)trialclient.getValues("instances");
						if (instances == null)
						{
							instances = new ArrayList<Data>();
						}
						instances.add(newinstance.getId());
						log.info("instanceid " + newinstance.getId());
						trialclient.setValue("instances", instances);
						clientsearcher.saveData(trialclient);
						*/
						
						context.putPageValue("userurl",fullURL);
						//context.putPageValue("client_name", organization);
						context.putPageValue("newuser", "admin");
						context.putPageValue("newpassword", "admin");
						
		                //Send Notification to us
						context.putPageValue("from", clientemail);
						context.putPageValue("subject", "New Activation - http://" + fullURL);
						sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/salesnotify.html");
						
						
						//Send Email to Client
						context.putPageValue("from", notifyemail);
						context.putPageValue("subject", "Welcome to EnterMediaDB ");
						sendEmail(context.getPageMap(),clientemail,"/entermediadb/app/site/sitedeployer/email/businesswelcome.html");
					}
				
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

protected Data addNewMonitor(Data server, String url, String client)
{
	Searcher monitorsearcher = searcherManager.getSearcher(catalogid, "monitoredsites");
	//TODO: set userid into client table
	Data newmonitor = monitorsearcher.createNewData();

	newmonitor.setValue("name",context.getRequestParameter("organization"));
	newmonitor.setValue("notifyemail",context.getRequestParameter("email"));
	newmonitor.setValue("server", server.name);
	newmonitor.setValue("isssl", "false");
	newmonitor.setValue("url", url);
	newmonitor.setValue("diskmaxusage", 95);
	newmonitor.setValue("memmaxusage", 200);
	newmonitor.setValue("monitoringenable", true);
	newmonitor.setValue("clientid", client);	
	monitorsearcher.saveData(newmonitor,null);
	return newmonitor;
}

protected Data addNewClient()
{
	Searcher clientsearcher = searcherManager.getSearcher(catalogid, "client");
	//TODO: set userid into client table
	Data newclient = clientsearcher.createNewData();

	newclient.setValue("name",context.getRequestParameter("organization"));
	newclient.setValue("timezone",context.getRequestParameter("timezone"));

	clientsearcher.saveData(newclient,null);
	return newclient;
}

protected Data addNewGroup()
{
	Searcher groupsearcher = searcherManager.getSearcher(catalogid, "group");
	Data group = groupsearcher.createNewData();
	group.setValue("id", context.getRequestParameter("organization"));
	group.setValue("name", context.getRequestParameter("organization"));
	groupsearcher.saveData(group,null);
	return group;
}

protected void addNewCollection(String url, String trialclient, String monitoredsite, String group)
{
	Searcher collectionsearcher = searcherManager.getSearcher(catalogid, "librarycollection");
	//TODO: set userid into client table
	Data newcollection = collectionsearcher.createNewData();

	Data library = mediaarchive.query("library").match("name", context.getRequestParameter("organization")).searchOne();
	
	if (library == null)
	{
		Searcher librarysearcher = searcherManager.getSearcher(catalogid, "library");
		library = librarysearcher.createNewData();
		
		library.setValue("name", context.getRequestParameter("organization"));
		library.setValue("owner", user.getId());
		library.setValue("viewgroups", group);
		librarysearcher.saveData(library,null);
	}
	
	
	newcollection.setValue("name", url);
	newcollection.setValue("library", library.getId());
	newcollection.setValue("timezone", context.getRequestParameter("timezone"));
	newcollection.setValue("owner", user.getId());
	newcollection.setValue("websitelink", "http://" + url);
	newcollection.setValue("isTrial", true);
	newcollection.setValue("startdate", new Date());
	newcollection.setValue("trial_clients", trialclient);
	newcollection.setValue("monitoredsites", monitoredsite);
	newcollection.setValue("startdate", new Date());
	newcollection.setValue("creationdate", new Date());
	newcollection.setValue("collectiontype", 1);
	newcollection.setValue("contactemail", context.getRequestParameter("email"));
	
	//TODO: deal with permissions (public based on group?)
	
	collectionsearcher.saveData(newcollection,null);
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