import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.entermediadb.websocket.chat.ChatManager
import org.openedit.Data

import org.openedit.data.BaseSearcher
import org.openedit.data.Searcher

public void init()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	String  collectionid = data.getId();
	
	LibraryCollection collection = (LibraryCollection)collectionsearcher.searchById(collectionid);
	
	//log.info("User is: " + user.getId() );

	//Create Orgainzations Library if does not exists
	BaseSearcher librarysearcher = mediaArchive.getSearcher("library");
	Data library = librarysearcher.searchById("organizations");
	if( library == null)
	{
		
		library = librarysearcher.createNewData();
		library.setId("organizations");
		library.setValue("owner", "admin");
		library.setName("Organizations");
		librarysearcher.saveData(library);
	}
	
	collection.setValue("library", library.getId());
	collection.setValue("organizationstatus", "active");
	if( collection.get("owner") == null )
	{
		collection.setValue("owner", user.getId());
	}	
	collectionsearcher.saveData(collection);
	context.putPageValue("librarycol", collection);
	log.info("Project created: ("+collectionid+") " + collection + " Owner: "+user.getId());
	
	//Create General Topic if not exists
	BaseSearcher colectivesearcher = mediaArchive.getSearcher("collectiveproject");
	Data newproject = colectivesearcher.query().exact("parentcollectionid",collection.getId()).match("name","General").searchOne();
	if( newproject == null)
	{
		newproject = colectivesearcher.createNewData();
		newproject.setName("General");
		newproject.setValue("parentcollectionid",collection.getId());
		colectivesearcher.saveData( newproject );
	}
	
	//--
	
	//Add user as Follower and Team
	Searcher librarycolusersearcher = mediaArchive.getSearcher("librarycollectionusers");
	Data userexists = librarycolusersearcher.query().exact("followeruser", user.getId()).exact("collectionid", collectionid).searchOne();
	Data librarycolusers = null;
	if (userexists == null) {
		librarycolusers = librarycolusersearcher.createNewData();
		librarycolusers.setValue("collectionid", collectionid);
		librarycolusers.setValue("followeruser", user.getId());
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
/*
	ChatManager chatmanager = (ChatManager) mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "chatManager");
	chatmanager.updateChatTopicLastModified(generaltopicid);
	
	mediaArchive.getProjectManager().getRootCategory(mediaArchive, collection);
	*/
}

init();



