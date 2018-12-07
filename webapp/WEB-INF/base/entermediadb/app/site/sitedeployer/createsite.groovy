import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.page.Page
import org.openedit.util.PathUtilities
import org.openedit.hittracker.*
import java.util.Random;

public void init() 
{

	Random rnd = new Random();
	Long random = Math.abs(System.currentTimeMillis() - rnd.nextLong());
	
	context.putSessionValue("clientform", random.toString());
	context.putPageValue("clientform", random.toString());
}

init();
