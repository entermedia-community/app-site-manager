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

public void init()
{
	String catalogid = "entermediadb/catalog";
	String notifyemail = "cristobal@entermediadb.org";
	
	String instanceid = context.getRequestParameter("instanceid"); 
	if (instanceid != null) {
		Searcher instanceSearcher = searcherManager.getSearcher(catalogid, "entermedia_instances");
		Data instance = instanceSearcher.searchById(instanceid);
		if (instance != null) {
			
			Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");
			Data server = serversSearcher.searchById(instance.entermedia_servers);
			if(server != null) {
				log.info("Deleting: " +instance.instancename+instance.instancenode+" on: "+server.sshname)
				ArrayList<String> command = new ArrayList<String>();
				command.add(server.sshname); //server name
				command.add(instance.instancename);  // Docker id
				command.add(instance.instancenode);  // Docker Node
				
				Exec exec = moduleManager.getBean("exec"); //removeclientinstance.sh m44 test 22
				ExecResult done = exec.runExec("removeclientinstance", command, true); //Todo: Need to move this script here?
			}
		}
	}
}