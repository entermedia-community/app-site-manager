import org.entermediadb.asset.MediaArchive
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public void removeinstance()
{
	log.info("Delete Instance");
	String catalogid = "entermediadb/catalog";
	String notifyemail = "cristobal@entermediadb.org";
	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	
	String instanceid = context.getRequestParameter("instanceid"); 
	if (instanceid != null) {
		Searcher instancesearcher = mediaarchive.getSearcher("entermedia_instances");
		Data instance =  instancesearcher.searchById(instanceid);
		
		
		if (instance != null && instance.get("instance_status") == 'deleted') {
			log.info("-- To Restore: id:" +instanceid+" Name:"+instance.getName());
			
			Searcher serverssearcher = mediaarchive.getSearcher("entermedia_servers");
			Data server = serverssearcher.searchById(instance.getValue("entermedia_servers"));
			if(server != null) {
				log.info("- Restoring: " +instance.getName()+" / "+instance.get("instancenode")+" on: "+server.get("sshname"))

				JSONObject jsonObject = new JSONObject();
				JSONArray jsonInstance = new JSONArray();
				JSONObject jsonInstanceObject = new JSONObject();
				
				jsonInstanceObject.put("subdomain", instance.instancename);
				jsonInstanceObject.put("containername", "t"+instance.instancenode);
				jsonInstance.add(jsonInstanceObject);
				
				jsonObject.put("assigned", jsonInstance);
				
				
				ArrayList<String> command = new ArrayList<String>();
			
				command.add("-i");
				command.add("/media/services/ansible/inventory.yml")
				command.add("/media/services/ansible/trial-recover.yml");
				command.add("--extra-vars");
				command.add("server=" + server.sshname + "");
				command.add("-e");
				command.add("" + jsonObject.toJSONString() + "");
	
				Exec exec = moduleManager.getBean("exec");
				ExecResult done = exec.runExec("trialsansible", command, true);
				
				
				server.setValue("currentinstances", server.getValue("currentinstances") + 1);
				serverssearcher.saveData(server);				
				instance.setValue("instance_status", "active");
				instancesearcher.saveData(instance);
		
			}
		}
	}
	log.info("No instance.");
}


public void init() {
	removeinstance();
}

init();