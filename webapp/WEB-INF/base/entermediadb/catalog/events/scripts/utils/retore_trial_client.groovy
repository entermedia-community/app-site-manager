package utils;

import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.entermediadb.asset.MediaArchive
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.openedit.*
import org.openedit.data.Searcher

public void init() {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	String instanceToRestore = context.getPageValue("collectionid");

	Searcher instanceSearcher = mediaArchive .getSearcher("entermedia_instances");
	// TODO, allow multiple restores
	Collection restoreInstances = instanceSearcher.query().exact("instance_status","deleted").and().exact("id", instanceToRestore).search();

	for (Iterator instanceIterator = restoreInstances.iterator(); instanceIterator.hasNext();)
	{
		Data instance = instanceSearcher.loadData(instanceIterator.next());
		
		searcherManager = context.getPageValue("searcherManager");
		Searcher serversSearcher = mediaArchive .getSearcher("entermedia_servers");
		Data server = serversSearcher.searchById(instance.entermedia_servers);
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

			serversSearcher.saveData(server);

			//Set Status active to Client
			instance.setProperty("instance_status", "active");
			instanceSearcher.saveData(instance, null);

		}
	}
}

init();
