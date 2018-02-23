package org.entermedia.sitemanager;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SiteModule extends BaseMediaModule
{

	private SiteManager manager;
	
	public void checkSites(WebPageRequest inReq)
	{
			String catalogid = inReq.findValue("catalogid");
			this.manager = (SiteManager)getModuleManager().getBean(catalogid,"siteManager");
			MediaArchive archive = getMediaArchive(inReq);

			manager.scan(archive);
	}
}

