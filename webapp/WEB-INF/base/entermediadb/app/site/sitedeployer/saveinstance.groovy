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
import org.openedit.util.StringEncryption
import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.entermediadb.websocket.chat.ChatManager
import org.openedit.page.Page
import org.openedit.util.PathUtilities

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



public void init() {
	
	String catalogid = "entermediadb/catalog";
	String notifyemail = "help@entermediadb.org";
	String clientemail = null;

/*	
	String clientform = context.getSessionValue("clientform");
	if (clientform != null) {
		context.putSessionValue("clientform", null);
	}
	else {
		context.putPageValue("errorcode", "1");
		return;
	}
	*/
	
	

	Map params = context.getSessionValue("userparams");
	String instanceurl = null;
	String region = null;
	String instancename = null;
	String organization_type = null;
	String userid = null;
	
	User user = context.getUser();
	if(user == null) {
		//Creating instance+user from email?
		user = getUser(context.getRequestParameter("email"));
		if(user == null) {
			context.putPageValue("status", "error");
			context.putPageValue("error", "Invalid Request");
			return;
		}
		else {
			userid = user.getId();
			clientemail = user.getEmail();
		}
	}
	else {
		clientemail = user.getEmail();
		if (params != null) {
			///Dialog Parameters
			log.info("creating site from dialog");
			instanceurl = params.get("organization_url");
			instancename = params.get("instancename");
			organization_type = params.get("organization_type");
			region = params.get("region");
			if (params.get("userid") != null) {
				userid = params.get("userid");
			}
		}
		if (params == null || instanceurl == null) {
			//Regular Form
			log.info("creating site from internal page");
			instanceurl = context.getRequestParameter("organization_url");
			instancename = context.getRequestParameter("instancename");
			organization_type = context.getRequestParameter("organization_type");
			region = context.getRequestParameter("region");
			if (context.getRequestParameter("userid") != null) {
				userid = context.getRequestParameter("userid");
			}
		}
	}
	if (instanceurl == null) {
		//Generate random instance url
		PasswordGenerator randomurl = new PasswordGenerator();
		instanceurl = randomurl.generate();
	}
	
	if (instancename == null) {
		instancename = instanceurl;
	}
	
	if (userid == null && user!= null) {
		userid = user.getId();
	}
	/*else {
		log.info("-Workspaces: No user defined.");
		context.putPageValue("status", "error");
		context.putPageValue("error", "No user defined");
		return;
	}*/
	
	
	String organizationid = context.getRequestParameter("collectionid");  //collectionid
	if (organizationid == null) {
		organizationid = createcollection(instancename, userid);
	}
	
	if (organizationid && instanceurl) {
		
		BaseSearcher collectionsearcher = mediaarchive.getSearcher("librarycollection");
		Data collection = collectionsearcher.searchByField("id", organizationid);
		if (collection) {
			context.putPageValue("organizationid", organizationid);
			context.putPageValue("organization", collection.getValue("name"));
		}
		
		//Create Valid URL
		String selected_url = instanceurl.toLowerCase();
		context.putPageValue("selected_url", selected_url);
				
		Searcher instancesearcher = searcherManager.getSearcher(catalogid, "entermedia_instances");
		Data newinstance = instancesearcher.createNewData();
		newinstance.setValue("librarycollection", organizationid);
		newinstance.setValue("owner", userid);
		newinstance.setValue("instance_status", "pending");
		newinstance.setValue("name", instancename); //Needs validation?
		newinstance.setValue("instanceprefix", selected_url);
		newinstance.setValue("istrial", true);
		instancesearcher.saveData(newinstance);
		
		context.putPageValue("instancename", instancename);
		
		HitTracker servers = null;
		
		if (region != null) {
			 servers = mediaarchive.query("entermedia_servers").exact("allownewinstances", true).match("server_region", region).search();
		}
		else {
			servers = mediaarchive.query("entermedia_servers").exact("allownewinstances", true).search();
		}
		
		Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");

		Data server = null;
		Integer nodeid = 0;
		
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
					foundspace = true;
					break;
				}
			 }
		
			if (!foundspace) {
				log.info("- No space on servers for trialsites");
				context.putPageValue("errorcode","2");
				context.putPageValue("status", "error");
				context.putPageValue("error", "No space on servers");
				//Send Email Notify No Space on Servers
				//context.putPageValue("from", clientemail);
				context.putPageValue("subject", "No space for Trial Sites");
				//sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/noseats.html");
			}
			else{
				// Call deploy script
				try {
					//Update Server
					currentinstances = currentinstances +1 ;
					server.setValue("currentinstances", currentinstances);
					nodeid = server.getValue("lastnodeid") + 1;
					selected_url = selected_url + "-" + String.valueOf(nodeid);
					server.setValue("lastnodeid", nodeid);
					serversSearcher.saveData(server);

					JSONObject jsonObject = new JSONObject();
					JSONArray jsonInstance = new JSONArray();
					JSONObject jsonInstanceObject = new JSONObject();
					
					jsonInstanceObject.put("subdomain", selected_url);
					jsonInstanceObject.put("containername", "t"+String.valueOf(nodeid));

					jsonInstance.add(jsonInstanceObject);
					jsonObject.put("assigned", jsonInstance);
					
					ArrayList<String> command = new ArrayList<String>();
					command.add("-i");
					command.add("/media/services/ansible/inventory.yml")
					command.add("/media/services/ansible/trial-assign.yml");
					command.add("--extra-vars");
					command.add("server=" + server.sshname + "");
					command.add("-e");
					command.add("" + jsonObject.toJSONString() + ""); //
					
					
					Exec exec = moduleManager.getBean("exec");
					ExecResult done = exec.runExec("trialsansible", command, true); //Todo: Need to move this script here?
					//ExecResult done = exec.runExec("/media/services/ansible/trialsansible.sh", command, true); //Todo: Need to move this script here?
					log.info("- Deploying Trial Site " + selected_url + " at " + server.getName());
					
						
					String fullURL = "https://" + selected_url + "." + server.trialdomain;
					
					newinstance.setValue("instanceurl", fullURL);
					newinstance.setValue("instance_status", "active");
					newinstance.setValue("instanceprefix", selected_url); //not need it?
					newinstance.setValue("instancename", selected_url);
					newinstance.setValue("instancenode", String.valueOf(nodeid));
					newinstance.setValue("istrial", true);
					newinstance.setValue("entermedia_servers", server.id);
					DateStorageUtil dateStorageUtil = DateStorageUtil.getStorageUtil();
					newinstance.setValue("datestart", new Date());
					newinstance.setValue("dateend", dateStorageUtil.addDaysToDate(new Date(), 30));
					instancesearcher.saveData(newinstance);
					
					context.putPageValue("status", "ok");
					context.putPageValue("instanceid", newinstance.getId());
					context.putPageValue("userurl", fullURL);
					context.putPageValue("instanceurl", fullURL);
					
					context.putPageValue("newuser", "admin");
					context.putPageValue("newpassword", "admin");
					
					//Add Site to Monitoring
					//Data monitor = addNewMonitor(newinstance);
					
					//Send Notification to us
					//context.putPageValue("from", clientemail);
					context.putPageValue("subject", "New Activation - " + fullURL);
					//sendEmail(context.getPageMap(), notifyemail,"/entermediadb/app/site/sitedeployer/email/salesnotify.html");				
					
					//Send Email to Client
					context.putPageValue("from", notifyemail);
					context.putPageValue("subject", "Welcome to EnterMediaDB ");
					
					context.putPageValue("entermediakey", getUserKey(user));
					
					sendEmail(context.getPageMap(),clientemail,"/entermediadb/app/site/sitedeployer/email/businesswelcome.html");
				
					}
					catch(Exception e){
						context.putPageValue("status", "error");
						context.putPageValue("error", "Unexpected Error");
						 e.printStackTrace();
					}
			
			}
			}
			else {
				log.info("- No Servers Available.");
				context.putPageValue("errorcode","3");
				context.putPageValue("status", "error");
				context.putPageValue("error", "No servers availavle");
				
			}
		
	}
	else {
		log.info("Missing: "+organizationid + " Region:" + region + " Instance:"+instanceurl);
		context.putPageValue("status", "error");
		context.putPageValue("error", "Missing Data");
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

public String createcollection(String inInstancename, String userid) {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	LibraryCollection  collection = collectionsearcher.createNewData();
	
	collection.setValue("name", inInstancename);
	collection.setValue("owner",userid);


	//Create Orgainzations Library if does not exists
	BaseSearcher librarysearcher = mediaArchive.getSearcher("library");
	String workspaceslibrary = context.getPageValue("workspaceslibrary");
	if (workspaceslibrary == null)
	{
		workspaceslibrary = "organizations";
	}
	Data library = librarysearcher.searchById(workspaceslibrary);
	if( library == null)
	{
		
		library = librarysearcher.createNewData();
		library.setId(workspaceslibrary);
		library.setValue("owner", "admin");
		library.setName("Organizations");
		librarysearcher.saveData(library);
	}
	
	collection.setValue("library", library.getId());
	collection.setValue("organizationstatus", "active");
	if( collection.get("owner") == null )
	{
		collection.setValue("owner", userid);
	}
	collectionsearcher.saveData(collection);
	String collectionid = collection.getId();
	context.putPageValue("librarycol", collection);
	log.info("Project created: ("+collectionid+") " + collection + " Owner: "+userid);
	
	//Create General Topic if not exists
	Searcher topicssearcher = mediaArchive.getSearcher("collectiveproject");
	Data topicexists = topicssearcher.query().exact("parentcollectionid",collectionid).searchOne();
	String generaltopicid=null;
	if (topicexists == null) {
		Data topic = topicssearcher.createNewData();
		topic.setValue("name", "General");
		topic.setValue("parentcollectionid", collectionid);
		topicssearcher.saveData(topic);
		generaltopicid = topic.getId();
	}
	else {
		generaltopicid = topicexists.getId();
	}
	
	
	//Add user as Follower and Team
	Searcher librarycolusersearcher = mediaArchive.getSearcher("librarycollectionusers");
	Data userexists = librarycolusersearcher.query().exact("followeruser", userid).exact("collectionid", collectionid).searchOne();
	Data librarycolusers = null;
	if (userexists == null) {
		librarycolusers = librarycolusersearcher.createNewData();
		librarycolusers.setValue("collectionid", collectionid);
		librarycolusers.setValue("followeruser", userid);
		librarycolusers.setValue("ontheteam","true");
		librarycolusersearcher.saveData(librarycolusers);
	}
	
	//Add Project Manager to Team
//	String projectmanager = "388";  // Rishi Bond
//	Data agentexists = librarycolusersearcher.query().exact("followeruser", projectmanager).exact("collectionid", collectionid).searchOne();
//	if (agentexists == null) {
//		librarycolusers = null;
//		librarycolusers = librarycolusersearcher.createNewData();
//		librarycolusers.setValue("collectionid", collectionid);
//		librarycolusers.setValue("followeruser", projectmanager);
//		librarycolusers.setValue("ontheteam","true");
//		librarycolusersearcher.saveData(librarycolusers);
//	}
	//--
		
	//Send Welcome Chat
//	Searcher chats = mediaArchive.getSearcher("chatterbox");
//	Data chat = chats.createNewData();
//	chat.setValue("date", new Date());
//	String welcomemessage = "Welcome to EnterMedia! My name is Rishi and I'm the Service Delivery Manager. My job is to make sure that you are happy with our product. Please take a look around and if you have any questions you can ask them here or email me at rishi@entermediadb.org. Looking forward to working with you!";
//	chat.setValue("message", welcomemessage);
//	chat.setValue("user", projectmanager);
//	chat.setValue("channel", generaltopicid);
//	chats.saveData(chat);
	//--

	ChatManager chatmanager = (ChatManager) mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "chatManager");
	chatmanager.updateChatTopicLastModified(generaltopicid);
	
	mediaArchive.getProjectManager().getRootCategory(mediaArchive, collection);
	
	return collectionid;
}


public User getUser(String email) {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	email = email.trim().toLowerCase();
	User theuser = null;
	theuser = mediaArchive.getUserManager().getUserByEmail(email);
	if( theuser != null)
	{
		log.info("Workspaces: User exists:"+theuser.getId());
	}
	else{
		//create user
			String	password = new PasswordGenerator().generate();
				
			theuser = mediaArchive.getUserManager().createUser(null, password);
			theuser.setEmail(email.trim().toLowerCase());
			theuser.setEnabled(true);
			mediaArchive.getUserManager().saveUser(theuser);
			log.info("Workspaces: New user created:"+theuser.getId());
	}
	return theuser;
}


public String getUserKey(User theuser) {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	String passenc = mediaArchive.getUserManager().getStringEncryption().getPasswordMd5(theuser.getPassword());
	passenc = theuser.getUserName() + "md542" + passenc;

	try
	{
		String tsenc = mediaArchive.getUserManager().getStringEncryption().encrypt(String.valueOf(new Date().getTime()));
		if (tsenc != null && !tsenc.isEmpty())
		{
			if (tsenc.startsWith("DES:"))
				tsenc = tsenc.substring("DES:".length());//kloog: remove DES: prefix since appended to URL
			passenc += StringEncryption.TIMESTAMP + tsenc;
		}
		else
		{
			log.info("Unable to append encrypted timestamp. Autologin URL does not have an expiry.");
		}
	}
	catch (OpenEditException oex)
	{
		log.error(oex.getMessage(), oex);
		log.info("Unable to append encrypted timestamp. Autologin URL does not have an expiry.");
	}
	return passenc;
	
}


init();


