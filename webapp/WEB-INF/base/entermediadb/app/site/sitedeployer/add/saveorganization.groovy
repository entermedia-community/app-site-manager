import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.entermediadb.websocket.chat.ChatManager
import org.openedit.Data

import org.openedit.data.BaseSearcher
import org.openedit.data.Searcher

public void init()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	String  collectionid = data.getId();
	LibraryCollection collection = (LibraryCollection)collectionsearcher.searchById(collectionid);
	Searcher librarysearcher = mediaArchive.getSearcher("library");
	log.info("User is: " + user.getId() );

	
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
	if( collection.get("owner") == null )
	{
		collection.setValue("owner", user.getId());
	}	
	collectionsearcher.saveData(collection);
	
	context.putPageValue("librarycol", collection);
	log.info("Collection saved: "+collection.getId());
	
	//Add user as Follower and Team
	Searcher librarycolusersearcher = mediaArchive.getSearcher("librarycollectionusers");
	Data librarycolusers = null;
	librarycolusers = librarycolusersearcher.createNewData();
	librarycolusers.setValue("collectionid", collectionid);
	librarycolusers.setValue("followeruser", user.getId());
	librarycolusers.setValue("ontheteam","true");
	librarycolusersearcher.saveData(librarycolusers);
	//--
	
	//First Create General Topic
	Searcher topics = mediaArchive.getSearcher("collectiveproject");
	Data topic = topics.createNewData();
	topic.setValue("name", "General");
	topic.setValue("parentcollectionid", collectionid);
	topics.saveData(topic);
	//--
	
	//Notify first project only
	Data collectionexists = librarycolusersearcher.query().exact("followeruser", user.getId()).searchOne();
	if (collectionexists != null) {
		String notifyuser = "168";  //Jay
		
		//Add Agent to Team
		librarycolusers = null;
		librarycolusers = librarycolusersearcher.createNewData();
		librarycolusers.setValue("collectionid", collectionid);
		librarycolusers.setValue("followeruser", notifyuser);
		librarycolusers.setValue("ontheteam","true");
		librarycolusersearcher.saveData(librarycolusers);
		//--
		
		//Send Welcome Chat
		Searcher chats = mediaArchive.getSearcher("chatterbox");
		Data chat = chats.createNewData();
		chat.setValue("date", new Date());
		chat.setValue("message", "Welcome to EnterMedia! My name is Jay and I'm here as your personal support agent. If you have any questions, I'm your man. Is there anything I can help you with?");
		chat.setValue("user", notifyuser);
		chat.setValue("channel", topic.getId());
		chats.saveData(chat);
		//--

		ChatManager chatmanager = (ChatManager) mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "chatManager");
		chatmanager.updateChatTopicLastModified(topic.getId());
	}
	mediaArchive.getProjectManager().getRootCategory(mediaArchive, collection);
}

init();



