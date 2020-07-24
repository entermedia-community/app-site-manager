import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil

import org.openedit.BaseWebPageRequest
import org.openedit.hittracker.*
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init() 
{
	String catalogid = "entermediadb/catalog";

	String projectname = context.getRequestParameter("name.value");
	HitTracker projects = mediaarchive.query("librarycollection").exact("name", projectname).search();

	if (projects != null && projects.size()>0) {
		context.putPageValue("result","false");
	}
	else {
		context.putPageValue("result","true");
	}
}


init();
