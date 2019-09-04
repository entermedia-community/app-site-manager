package org.entermedia.autofailover;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class AutoFailoverModule extends BaseMediaModule
{
	public void createDNSRecord(WebPageRequest inReq)
	{
//		String catalogid = inReq.findValue("catalogid");
//		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");
//
//		manager.createRecord("thomas", "CNAME", "openinstitute.org", "mediadb36.entermediadb.net", null, null, 10);
	}

	public void updateDNSRecord(WebPageRequest inReq)
	{
//		String catalogid = inReq.findValue("catalogid");
//		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");
//
//		manager.updateRecord("thomas", "test.api2.com", null, 60, 10);
	}

	public void forceUpdateDNSRecord(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");

		manager.forceLeaveFailover();
	}
	
	public void getDNSRecords(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");

		manager.getList("entermediacloud.com");
	}
	
	public void initGeoLatencyRules(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AutoFailoverManager manager = (AutoFailoverManager) getModuleManager().getBean(catalogid, "autoFailoverManager");

		manager.initGeoLatencyRules();		
	}
}
