package util;

import org.openedit.*;
import org.openedit.data.*;
import org.entermediadb.asset.*;
import org.openedit.hittracker.*;

public void go()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher spsearcher = mediaArchive.getSearcher("sessionpackages");
        Collection all = spsearcher.query().all().search();
	for(Data spackage : all)
	{
		Collection sessioncollection = mediaArchive.getSearcher("sessions").query().match("sessionpackageid",spackage.getId()).search();
		int complete = 0;
                log.info("sessioncollection" + sessioncollection.size() );
		for( Data asession : sessioncollection)
		{
			//log.info(asession);
			String status = asession.get("session_status");
			if( status == "AWHEBvr_vTYoF1U6IcBo" || status == "AWH37884DjKvO0yVUldK" )
			{
				complete++;
			}
		}
		spackage.setValue("sessionsused",complete);
		spsearcher.saveData(spackage);
	        log.info( " " + spackage + " completed " + complete);
	}
}

go();
