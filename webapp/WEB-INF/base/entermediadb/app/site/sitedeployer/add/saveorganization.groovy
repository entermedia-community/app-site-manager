import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
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
	
	mediaArchive.getProjectManager().getRootCategory(mediaArchive, collection);
	
	
	BaseSearcher colectivesearcher = mediaArchive.getSearcher("collectiveproject");
	Data newproject = colectivesearcher.createNewData();
	newproject.setName("General");
	newproject.setValue("parentcollectionid", collection.getId());
	colectivesearcher.saveData( newproject );
	
	
}

init();