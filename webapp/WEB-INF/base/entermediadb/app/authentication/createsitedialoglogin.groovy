import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.page.Page
import org.openedit.util.PathUtilities
import org.openedit.hittracker.*
import java.util.Random;

public void init() 
{
/* Save variables */
	Map params = context.getParameterMap();
	context.putSessionValue("userparams", params);
/* Redirect */
	String confirmpage = context.findValue("confirmpage");
	context.putSessionValue("fullOriginalEntryPage", confirmpage);
	
}

init();
