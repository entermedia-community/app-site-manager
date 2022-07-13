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
	SetLastLoginDefault(mediaArchive, instanceSearcher);

	Calendar limit = Calendar.getInstance();
	limit.add(Calendar.DAY_OF_YEAR, -30);
	Collection expiredInstances = instanceSearcher.query()
									.exact("istrial", true)
									.exact("instance_status","active")
									.before("lastlogin", limit.getTime()).search();
    log.info(expiredInstances);
	if (!expiredInstances.size()) {
		expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "todelete").search();
	}
	log.info("Found "+expiredInstances.size()+" sites to delete.");

	for (Iterator instanceIterator = expiredInstances.iterator(); instanceIterator.hasNext();) {
		//Get The Client
		Data instance = instanceSearcher.loadData(instanceIterator.next());

		//Get Server Info
		searcherManager = context.getPageValue("searcherManager");
		Searcher serversSearcher = mediaArchive .getSearcher("entermedia_servers");
		Data server = serversSearcher.searchById(instance.entermedia_servers);
		if (server) {
			log.info("Deleting instance: " + instance.name+",  on server "+server.name);
			String instacename = instance.instanceprefix + "";

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
			Exec exec = moduleManager.getBean("exec");
			ExecResult done = exec.runExec("trialsansible", command, true);

			//Discount currentinstances on server
			if(instance.getValue("instance_status") != 'deleted') {
				server.setValue("currentinstances", server.getValue("currentinstances") - 1);
			}
			serversSearcher.saveData(server);


			//Set Status Deleted to Client
			instance.setProperty("instance_status","deleted");
			instanceSearcher.saveData(instance, null);

			//Delete Monitoring
			Collection todelete = new ArrayList();
			Searcher monitorsearcher = mediaArchive.getSearcher("entermedia_instances_monitor");
			Data instancemonitor = monitorsearcher.query().match("instanceid", instance.id).searchOne();
			if (instancemonitor) {
				//instancemonitor.setValue("monitoringenable","false");
				//monitorsearcher.saveData(instancemonitor, null);
				todelete.add(instancemonitor);
			}
			monitorsearcher.deleteAll(todelete, null);
		}
		else {
			log.info("Can't delete instance: "+instance.getName());
		}
	}
}

public void SetLastLoginDefault(MediaArchive mediaArchive, Searcher instanceSearcher) {
	Collection instances = instanceSearcher.query().exact("istrial", "true").exact("instance_status","active").search();
	// TODO: refine query. just search for lastlogin == null or empty
	for (Iterator instanceIterator = instances.iterator(); instanceIterator.hasNext();)
	{
		Data instance = mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.next());
		if (instance.lastlogin == null) {
			instance.setValue("lastlogin", new Date());
			instanceSearcher.saveData(instance, null);
		}
	}
}


init();
