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



public void init() 
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");

	String collectionid = context.getRequestParameter("collectionid");
	String newlabel = context.getRequestParameter("newname");
	if( newlabel == null)
	{
		context.putPageValue("status","newnamemissing");
		return;
	}
	Data collection = mediaArchive.getData("librarycollection",collectionid);
	if( collection != null)
	{
		collection.setName(newlabel);
		mediaArchive.saveData("librarycollection",collection);
		context.putPageValue("status","complete");
		return;
	}
	else
	{
		context.putPageValue("status","nosuchcollection");
	}
}

init();


