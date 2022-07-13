package utils;

import org.entermediadb.asset.MediaArchive
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public void init() {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	Searcher instanceSearcher = mediaArchive .getSearcher("entermedia_instances");
	Collection restoreInstances;	
	
	restoreInstances = instanceSearcher.match("instance_status","torestore").search();	

	log.info("Found "+restoreInstances.size()+" sites to restore.");
	for (Iterator instanceIterator = restoreInstances.iterator(); instanceIterator.hasNext();) {
		Data instance = mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.next());
		log.info("server: "+ instance.entermedia_servers);
		
		Data server = mediaArchive.getSearcher("entermedia_servers").searchById(instance.entermedia_servers);
		
		if (server) {
			log.info("Restoring instance: " + instance.name + ", on server " + server.name);
			String instacename = instance.instanceprefix + "";

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

			//Set Status active to Client
			instance.setProperty("instance_status", "active");
			instanceSearcher.saveData(instance, null);

		}
	}
	log.info("Finished restoring all trials");
}

init();
