package org.entermedia.autofailover;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class AutoFailoverModule extends BaseMediaModule
{
	public void createDNSRecord(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");

		manager.createRecord("thomas", "openinstitute.org", "thomasopeninstitute.org", null, 60, 0);
	}
}
