package org.entermedia.speedtest;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SpeedModule extends BaseMediaModule
{
	public void checkSpeeds(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SpeedManager manager = (SpeedManager) getModuleManager().getBean(catalogid, "speedManager");
		MediaArchive archive = getMediaArchive(inReq);

		manager.checkSpeed(archive);
	}
}
