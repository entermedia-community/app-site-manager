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
	User user = context.getUser();
	if(user == null) {
		context.putPageValue("status","error");
		log.info("No user");
		return
	}
	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	Searcher collectionsearcher = mediaarchive.getSearcher("librarycollection");

	String collectionid = context.getRequestParameter("collectionid");
	LibraryCollection collection = (LibraryCollection)collectionsearcher.searchById(collectionid);
	if( collection != null)
	{
		
		//Search Instance
		Searcher instancesearcher = mediaarchive.getSearcher("entermedia_instances");
		Data instance = null;
		if ("admin".equals(user.getId())) {
			instance = instancesearcher.query().match("librarycollection", collectionid).searchOne();
		}
		else {
			instance = instancesearcher.query().match("librarycollection", collectionid).exact("owner", user.getId()).searchOne();
		}
		if (instance) {
			collection.setValue("organizationstatus","pendingdelete");
			collectionsearcher.saveData(collection);
	
			context.putPageValue("instanceid",instance.getId());
			log.info("Deleting Intance: "+instance.getId());
			removeinstance();
			context.putPageValue("status","complete");
			return;
		}
		context.putPageValue("status","nopermissions");
		log.info("No permissions. User: "+user.getId()+" Deleting: "+collectionid);
		return;
	}
	else
	{
		context.putPageValue("status","nosuchcollection");
	}
}


public void removeinstance()
{
	
	String catalogid = "entermediadb/catalog";
	String notifyemail = "cristobal@entermediadb.org";
	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	
	String instanceid = context.getRequestParameter("instanceid");
	if (instanceid == null) {
		instanceid = context.getPageValue("instanceid");
	}
	if (instanceid != null) {
		Searcher instancesearcher = mediaarchive.getSearcher("entermedia_instances");
		Data instance =  instancesearcher.searchById(instanceid);
		
		
		if (instance != null && !"deleted".equals(instance.get("instance_status"))) {
			log.info("-- to delete: id:" +instanceid+" Name:"+instance.getName());
			
			Searcher serverssearcher = mediaarchive.getSearcher("entermedia_servers");
			Data server = serverssearcher.searchById(instance.get("entermedia_servers"));
			if(server != null) {
				log.info("- Deleting: " +instance.getName()+" / "+instance.get("instancenode")+" on: "+server.get("sshname"))

				JSONObject jsonObject = new JSONObject();
				JSONArray jsonInstance = new JSONArray();
				JSONObject jsonInstanceObject = new JSONObject();
				
				jsonInstanceObject.put("subdomain", instance.instancename);
				jsonInstanceObject.put("containername", "t"+instance.instancenode);
				jsonInstance.add(jsonInstanceObject);
				
				jsonObject.put("deleted", jsonInstance);
				
				
				ArrayList<String> command = new ArrayList<String>();
			
				command.add("-i");
				command.add("/media/services/ansible/inventory.yml")
				command.add("/media/services/ansible/trial.yml");
				command.add("--extra-vars");
				command.add("server=" + server.sshname + "");
				command.add("-e");
				command.add("" + jsonObject.toJSONString() + "");
	
				
				Exec exec = moduleManager.getBean("exec"); //removeclientinstance.sh m44 test 22
				ExecResult done = exec.runExec("trialsansible", command, true); //Todo: Need to move this script here?
				
				//Discount currentinstances on server
				if(instance.getValue("instance_status") == 'active') {
					server.setValue("currentinstances", server.getValue("currentinstances") - 1);
				}
				serverssearcher.saveData(server);
				
				instance.setValue("instance_status", "deleted");
				instancesearcher.saveData(instance);
				
			}
			else {
				log.info("- server not found:" + instance.get("entermedia_servers"))
			}
		}
	}
	else {
		log.info("No instance.");
	}
}


init();


