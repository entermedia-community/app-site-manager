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
import org.openedit.util.URLUtilities
import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.entermediadb.websocket.chat.ChatManager
import org.openedit.page.Page
import org.openedit.util.PathUtilities

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



public void init() 
{
	log.info(context.getUser());
	if( context.getUser() == null )
	{
		log.info("User key did not work for login ");
		//TODO: Should redirect to the login form
		context.redirect("./authentication/login.html");
		return;
	}
	
	Collection organizationsusers = mediaarchive.query("librarycollectionusers").exact("followeruser",user.getId()).exact("ontheteam","true").search();
	
    List oids = new ArrayList();
	for( Data data in organizationsusers)
	{
		oids.add(data.collectionid);
	}
	oids.add("NONE");
	log.info("Found " + oids.size() + " for user " + user.getId());
	
	def collections = mediaarchive.query("librarycollection").ids(oids).not("organizationstatus","disabled").not("organizationstatus","closed").not("organizationstatus","pendingdelete").sort("name").search();
	
    def servers = null;
	if( !collections.isEmpty() )
	{
		servers = mediaarchive.query("entermedia_instances").orgroup("librarycollection", collections).exact("instance_status","active").search();
	}
	log.info("Found " + collections.size() + " Active Collections for user " + user.getId());
	
	String collectionid = null;
	
	if( servers == null || servers.isEmpty()) 
	{
		PasswordGenerator randomurl = new PasswordGenerator();
		String instancename = randomurl.generate();
		String newcollectionid = createcollection(instancename,user.getId());
		Map params = new HashMap();
		params.put("collectionid",newcollectionid);
		
		mediaarchive.fireGeneralEvent(user,"sitedeployer","deployinstance",params);
		
        servers = mediaarchive.query("entermedia_instances").exact("librarycollection", newcollectionid).match("instance_status","active").search();
		
		collectionid = newcollectionid;
	}
	if( servers.isEmpty())
	{
		//throw new OpenEditException("Server could not be deployed"); //Never Empty
		context.redirect("error.html");
		return;
	}
	
	//Redirects to first available and track login
	BaseSearcher instancesearcher = mediaarchive.getSearcher("entermedia_instances");
	Date lastlogin = new Date();
	def mostrecent = servers.first();
	mostrecent.setValue("lastlogin",lastlogin);
	instancesearcher.saveData(mostrecent);
	
	String url = mostrecent.get("instanceurl");
	String entermediakey = context.getRequestParameter("entermedia.key");
	if (collectionid == null) {
		collectionid = mostrecent.get("librarycollection");
	}
	if( entermediakey == null)
	{
		entermediakey = mediaarchive.getUserManager().getEnterMediaKey(user);
	}
	
	Boolean noredirect = Boolean.parseBoolean(context.getRequestParameter("noredirect"));
	if(noredirect) {
		return;
	}
	
	//TODO: Enable user
	String link = url + "/finder/find/startmediaboat.html?entermediacloudkey=" + entermediakey + "&collectionid=" + collectionid
	String encoded = URLUtilities.urlEscape(link);
	log.info("Sending to " + encoded);
	context.redirect(encoded);

	
}

public String createcollection(String inInstancename, String userid) {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	LibraryCollection  collection = collectionsearcher.createNewData();
	
	collection.setValue("name", inInstancename);
	collection.setValue("owner",userid);


	//Create Orgainzations Library if does not exists
	BaseSearcher librarysearcher = mediaArchive.getSearcher("library");
	String workspaceslibrary = context.findValue("workspaceslibrary");
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

//	ChatManager chatmanager = (ChatManager) mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "chatManager");
//	chatmanager.updateChatTopicLastModified(generaltopicid);
	
	mediaArchive.getProjectManager().getRootCategory(mediaArchive, collection);
	
	return collectionid;
}


init();


