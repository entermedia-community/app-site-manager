package org.entermedia.speedtest;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SpeedTestModule extends BaseMediaModule
{
	public void checkSpeeds(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SpeedTestManager manager = (SpeedTestManager) getModuleManager().getBean(catalogid, "speedTestManager");
		MediaArchive archive = getMediaArchive(inReq);

		manager.checkSpeed(archive);
	}
}
