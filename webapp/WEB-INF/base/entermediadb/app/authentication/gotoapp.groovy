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
	
	Collection organizationsusers = mediaarchive.query("librarycollectionusers").exact("followeruser",user.getId()).exact("ontheteam","true").search() );
	
    List oids = new ArrayList();
	for( Data data in organizationsusers)
	{
		oids.add(data.collectionid))
	}
	oids.add("NONE");
	def searcher = mediaarchive.getSearcher("searcher") )
	def collections = mediaarchive.query("librarycollection").ids(oids).not("organizationstatus","disabled").not("organizationstatus","closed").not("organizationstatus","pendingdelete").sort("name").search() );
	
    def servers = mediaarchive.query("entermedia_instances").orgroup("librarycollection", collections).search() );
	if( servers.isEmpty()) {
		mediaarchive.fireEvent("deployinstance");
        servers = mediaarchive.query("entermedia_instances").orgroup("librarycollection", collections).search() );
	}

	def mostrecent = servers.first();
	context.redirect(mostrecent.get("instanceurl"));
	
}


init();


