package org.entermedia.sslcertificates;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SSLModule extends BaseMediaModule
{
	public void checkSSL(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SSLManager manager = (SSLManager) getModuleManager().getBean(catalogid, "sslManager");
		MediaArchive archive = getMediaArchive(inReq);

		manager.checkExpirationDate(archive);
	}
}
