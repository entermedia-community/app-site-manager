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
	User user = context.getUser();
	if(user == null) {
		context.putPageValue("status","error");
		return
	}
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");

	String collectionid = context.getRequestParameter("collectionid");
	Data collection = mediaArchive.getData("librarycollection",collectionid);
	if( collection != null)
	{
		
		//Search Instance
		Data instance = null;
		instance = mediaArchive.query("entermedia_instances").match("librarycollection", collectionid).exact("owner", user.getId()).searchOne();
		if (instance) {
			collection.setValue("organizationstatus","pendingdelete");
			mediaArchive.saveData("librarycollection",collection);
	
			context.putPageValue("instanceid",instance.getId());
			log.info("Deleting Intance: "+instance.getId());
			removeinstance();
			context.putPageValue("status","complete");
			return;
		}
		context.putPageValue("status","nopermissions");
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
		
		
		if (instance != null && instance.get("instance_status") != 'deleted') {
			log.info("-- To delete: id:" +instanceid+" Name:"+instance.getName());
			
			Searcher serverssearcher = mediaarchive.getSearcher("entermedia_servers");
			Data server = serverssearcher.searchById(instance.getValue("entermedia_servers"));
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
		}
	}
	log.info("No instance.");
}


init();


