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
    String notifyemail = "help@entermediadb.org"


	
	String clientform = context.getSessionValue("clientform");
	if (clientform != null) {
		context.putSessionValue("clientform", null);
	}
	else {
		context.putPageValue("errormsg","Invalid Submission. Please try <a href='./createsite.html'>again</a>.");
		return;
	}

	String organization = context.getRequestParameter("organization");
	String email = context.getRequestParameter("email");

	//String email = user.getEmail();
	if (organization != "" && email != "") {
		email = email.toLowerCase();
		String instanceurl = context.getRequestParameter("organization_url");
		String organization_type = context.getRequestParameter("organization_type");
        String timezone = context.getRequestParameter("timezone");	
		String region = context.getRequestParameter("region");
		
		//
		// String clientsubdomain = context.getRequestParameter("clientsubdomain");
		//
		Searcher clientsearcher = searcherManager.getSearcher(catalogid, "entermedia_clients");
		//TODO: set userid into client table
		Data newclient = clientsearcher.createNewData();

		Searcher instancesearcher = searcherManager.getSearcher(catalogid, "entermedia_instances");
		Data newinstance = instancesearcher.createNewData();

		newclient.setName(organization);
		
		//Validate Email
		String email_regex = /[_A-Za-z0-9-]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})/;
		if (email ==~ email_regex) {
			newclient.setProperty("clientemail",email);  
		}
		else {
			context.putPageValue("errormsg","Invalid Email.");
			info.log("Invalid Email ");
			return;
		}
		
//		newclient.setValue("organization",organization);
		// use NAME insted of organization ?
		newclient.setValue("clientcategory", organization_type);
		newclient.setValue("timezone",timezone);
//		newclient.setValue("region",region);
		
		clientsearcher.saveData(newclient,null);
		
		//Create Valid URL
        String selected_url = instanceurl.toLowerCase();
		
		context.putPageValue("selected_url", selected_url);
        context.putPageValue("organization", organization);



		//Get server(s) by region
		log.info("- Checking region" + region);
		
 		HitTracker servers = mediaarchive.query("entermedia_servers").match("server_region", region).search();
		Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");

		Data seat = null;
		Data server = null;
		//Check if server's seat has room
		for (Iterator serverIterator = servers.iterator(); serverIterator.hasNext();)
		{
			server = serversSearcher.loadData(serverIterator.next());
			log.info("- Checking server " + server.getName());
			HitTracker hits = mediaarchive.query("entermedia_seats").match("seatstatus", "false").match("entermedia_servers", server.id).search();			
			if ( hits.size() < Integer.parseInt(server.maxinstance) )
			{
				seat = mediaarchive.query("entermedia_seats").match("seatstatus", "false").match("entermedia_servers", server.id).searchOne();
				break;
			}
		}

        log.info(seat);
		
		if (seat != null)
		{
			//Assign client to seat
			seat.setValue("clientid",newclient.id);
			seat.setValue("seatstatus","true");
			mediaarchive.saveData("entermedia_seats", seat);
			
			
			//Get Server Info
			log.info("Getting " + server.name + " info");
			
				
			// Call deploy script 
			try {
				List<String> command = new ArrayList<String>();
				command.add(server.sshname); //server name
				command.add(server.dockersubnet);  //server subnet
				command.add(selected_url);  //client url
				command.add(String.valueOf(seat.nodeid));  //client nodeid				
				command.add(server.getValue("serverurl"));  // DNS
				
				Exec exec = moduleManager.getBean("exec");
				ExecResult done = exec.runExec("setupclient", command);
				log.info("Exec: " + done.getStandardOut());
				
				Data trialclient = clientsearcher.query().match("id", newclient.id).searchOne();
					
				String fullURL = selected_url + "." + server.serverurl;
				
				//TODO: missing ClientID
				Data client = mediaarchive.query("client").match("name", organization).searchOne();
				Data group = null;
				if ( client == null )
				{
					log.info("Client not found, creating new client and group");
					client = addNewClient();
					group = addNewGroup();
					Searcher userSearcher = searcherManager.getSearcher(catalogid, "user");
					
					Collection groups = (Collection)user.getAt("groups");
					groups.add(group.getId());
					user.setValue("groups", groups);
					userSearcher.saveData(user,null);
				}
                else {
                    //Account exists, send error message to let them know
					group = mediaarchive.query("group").match("name", organization).searchOne();
                }
			

                //Add Site to Monitoring
				Data monitor = addNewMonitor(server, "http://" + fullURL, client.id);
				addNewCollection(fullURL, newclient.id, monitor.id, group.id);

				newinstance.setValue("instanceurl", fullURL);
				newinstance.setValue("instance_status", "active");
				newinstance.setValue("istrial", true);
				newinstance.setValue("entermedia_servers", server.id);
				//newclient.setProperty("datestart",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				
				DateStorageUtil dateStorageUtil = DateStorageUtil.getStorageUtil();
				newinstance.setValue("datestart", new Date());
				newinstance.setValue("dateend", dateStorageUtil.addDaysToDate(new Date(), 30));
				
				instancesearcher.saveData(newinstance,null);
		
				
				context.putPageValue("userurl",fullURL);
				context.putPageValue("client_name", name);
				context.putPageValue("newuser", "admin");
				context.putPageValue("newpassword", "admin");
				
                //Send Notification to us
				context.putPageValue("from", email);
				context.putPageValue("subject", "New Activation - http://" + fullURL);
				sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/salesnotify.html");
				
				
				//Send Email to Client
				context.putPageValue("from", notifyemail);
				context.putPageValue("subject", "Welcome to EnterMediaDB " + name);
				sendEmail(context.getPageMap(),email,"/entermediadb/app/site/sitedeployer/email/businesswelcome.html");
				
				
			}
			catch(Exception e){
				 e.printStackTrace();
			}
			
		}
		else {
			log.info("No seats available");
			context.putPageValue("errormsg","No Demo sites available for now. Please contact EnterMedia support team.");
            
            //Send Email Notify No Seats
            context.putPageValue("from", email);
            context.putPageValue("subject", "No Seats Available for Trial Sites");
            sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/noseats.html");
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