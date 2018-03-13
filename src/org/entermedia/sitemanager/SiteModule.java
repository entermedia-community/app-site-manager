package org.entermedia.sitemanager;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SiteModule extends BaseMediaModule
{
	public void checkSites(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SiteManager manager = (SiteManager) getModuleManager().getBean(catalogid, "siteManager");
		MediaArchive archive = getMediaArchive(inReq);

		manager.scan(archive);
	}

	public void checkVersions(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SiteManager manager = (SiteManager) getModuleManager().getBean(catalogid, "siteManager");
		MediaArchive archive = getMediaArchive(inReq);

		manager.scanSoftwareVersions(archive);
	}

}
