import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.json.simple.JSONObject
import org.entermediadb.asset.MediaArchive

public void init() {
	String collectionid = context.getPageValue("collectionid");
	String entermediacloudkey = context.getPageValue("entermediacloudkey");
	
	if (collectionid == null || entermediacloudkey == null) {
		RequestHasError(null, "Must have collectionid and entermediacloudkey");
		return;
	}
	
	String adminRequest = "/workspaces/getadminkey.json?collectionid=" + collectionid + "&entermediacloudkey=" + entermediacloudkey;
	JSONObject keyRequest = HttpPost(adminRequest);
	String emKey;
	
	if (RequestHasError(keyRequest, "Could not get key")) {
		return;
	}
	JSONObject response = (JSONObject)keyRequest.get("response");
	emKey = (String)response.get("entermediakey");	
	
	JSONObject reindexReq = HttpPost("/system/reindex.json?entermedia.key=" + emKey);
	if (RequestHasError(reindexReq, "Could not reindex.json")) {
		return;
	}
	response = (JSONObject)reindexReq.get("response");
	String time = (String)response.get("time");
	
	context.putPageValue("status", "ok");
	context.putPageValue("time", time);

}

private JSONObject HttpPost(String request) {
	String host = context.getPageValue("host");
	String catalog = context.getPageValue("catalog");
	if (catalog == null || catalog.isEmpty()) {
		catalog = "finder"
	}
	String req = "/mediadb/services";
	String url = host + "/" +catalog + req + request;

	CloseableHttpResponse resp = getConnection().sharedPostWithJson(url, params);
	StatusLine filestatus = resp.getStatusLine();
	if (filestatus.getStatusCode() != 200)
	{
		log.info( filestatus.getStatusCode() + " URL issue " + " " + url + " with " + userkey);
		inReq.setCancelActions(true);
		return null;
	}
	JSONObject data = getConnection().parseJson(resp);
	return data;
}

private boolean RequestHasError(JSONObject jsonRequest, String reason) {
	if (jsonRequest == null) {
		context.putPageValue("status", "Error");
		context.putPageValue("reason", reason);
		return true;
	}
	JSONObject response = (JSONObject)keyRequest.get("response");
	String status = (String)response.get("status");
	if (!status.equals("ok")) {
		context.putPageValue("status", "Error");
		context.putPageValue("reason", reason);
		return true;
	}
	
	return false;
}