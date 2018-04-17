import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil

import org.openedit.BaseWebPageRequest
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init() 
{
	String catalogid = "sitemanager/catalog";

	String instanceurl = context.getRequestParameter("organization_url");
	
	Searcher clientsearcher = searcherManager.getSearcher(catalogid, "trial_clients");

	SearchQuery cquery = clientsearcher.createSearchQuery();
	HitTracker seats = clientsearcher.search(cquery);
	Collection hits = clientsearcher.query().match("instanceurl",instanceurl).search()

	if (hits) {
		context.putPageValue("result","false");
	}
	else {
		context.putPageValue("result","true");
	}
}


init();
