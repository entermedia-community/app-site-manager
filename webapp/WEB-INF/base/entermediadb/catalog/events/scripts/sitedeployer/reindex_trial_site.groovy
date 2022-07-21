package sitedeployer;

import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.entermediadb.asset.MediaArchive
import org.entermediadb.net.HttpSharedConnection
import org.json.simple.JSONObject
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.User

public void init() {
	assignWeeklyPending();

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Collection restoreInstances = mediaArchive.query("entermedia_instances")
								.exact("instance_status", "active")
								.exact("instance_reindex_status", "pending").search();
	log.info("reindexing " + restoreInstances.size() + " servers")

	for (Iterator instanceIterator = restoreInstances.iterator(); instanceIterator.hasNext();) {
		Data instance = mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.next());
		if (instance!= null && instance.get("instanceurl")!= null)
		{
			String ownerId = instance.get("owner");
			User owner = mediaArchive.getUser(ownerId);
			String cloudKey = mediaArchive.getUserManager().getEnterMediaKey(owner);
	
			log.info("Reindex instance: " + instance.name);
			reIndexTrial(mediaArchive, instance, cloudKey);
		}
		else 
		{
			log.info("Error reindexing instance: " + instance.name);
		}
	}
}

private void reIndexTrial(MediaArchive mediaArchive, Data instance, String entermediacloudkey) {
	try {
		Map<String, String> params = new  HashMap();
		String collectionId = instance.get("librarycollection");
		params.put("collectionid", collectionId);
		params.put("entermediacloudkey", entermediacloudkey);
		String instanceurl = instance.get("instanceurl");
		log.info("Reindex request: "+instanceurl+params);
		JSONObject reindexReq = httpPost("/workspaces/reindex.json", instanceurl, params);
		if (reindexReq != null)
		{
			JSONObject response = (JSONObject)reindexReq.get("response");
			if (response != null) 
			{
				log.info("Reindex took:" + (String)response.get("time")+"s");
				if (!response.get("status").equals("ok")) {
					instance.setValue("instance_reindex_status", "error");
				} else {
					instance.setValue("instance_reindex_status", "complete");
					instance.setValue("lastreindexdate", new Date());
				}
			}
		}
	} catch (Exception e) {
		instance.setValue("instance_reindex_status", "error");
		//log.error(e);
		throw new OpenEditException(e);
		
	}
	mediaArchive.saveData("entermedia_instances", instance);
}

private JSONObject httpPost(String request, String host, Map params) {
	HttpSharedConnection fieldConnection = new HttpSharedConnection();
	String catalog = "finder";
	if (catalog == null || catalog.isEmpty()) {
		catalog = "finder"
	}
	String req = "/mediadb/services";
	String url = host + "/" +catalog + req + request;

	log.info("url:"+ url);
	log.info("param:"+ params);

	CloseableHttpResponse resp = fieldConnection.sharedPostWithJson(url, new JSONObject(params));
	StatusLine filestatus = resp.getStatusLine();
	if (filestatus.getStatusCode() != 200) {
		log.info( filestatus.getStatusCode() + " URL issue " + "params:" + params);
		return null;
	}
	JSONObject data = fieldConnection.parseJson(resp);
	return data;
}

private void assignWeeklyPending() {
	log.info("Checking Weekly");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	setLastReIndexDateDefault(mediaArchive);
	Calendar limit = Calendar.getInstance();
	limit.add(Calendar.DAY_OF_YEAR, -7)
	Collection restoreInstances = mediaArchive.query("entermedia_instances").exact("instance_status", "active").exact("istrial", true).before("lastreindexdate", limit.getTime()).search();
	log.info("checking before:" + limit.getTime());
	log.info("found:" + restoreInstances.size());

	for (Iterator instanceIterator = restoreInstances.iterator(); instanceIterator.hasNext();) {
		Data instance = mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.next());
		if(instance != null)
		{
			log.info("Setting to reindex, instance: " + instance.name);
			instance.setValue("instance_reindex_status", "pending");
			mediaArchive.saveData("entermedia_instances", instance);
		}
	}
}

public void setLastReIndexDateDefault(MediaArchive mediaArchive) {
	Collection instances = mediaArchive.query("entermedia_instances").exact("istrial", "true").exact("instance_status","active").search();
	// TODO: refine query. just search for lastlogin == null or empty
	for (Iterator instanceIterator = instances.iterator(); instanceIterator.hasNext();)
	{
		Data instance = mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.next());
		
		if (instance != null && instance.lastreindexdate == null) {
			instance.setValue("lastreindexdate", new Date());
			mediaArchive.saveData("entermedia_instances", instance);
		}
	}
}

init();