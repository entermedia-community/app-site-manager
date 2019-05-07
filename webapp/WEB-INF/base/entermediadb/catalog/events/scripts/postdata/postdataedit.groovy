package postdata

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.hittracker.HitTracker

public void init()
{
    MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
    
    HitTracker all = archive.query("postdata").startsWith("sourcepath", "knowledge").search();
    all.enableBulkOperations();
    for(Data post in all)
    {
        //String cat = post.get("knowledgebase_category");
        String content = post.get("sourcepath");
        //if( cat != null)
            log.info("content: " + content);
	if ( content != null )
        {
            //cat = cat.toLowerCase();
	    //cat = cat.replaceAll(" ", "");
            //post.setValue("knowledgebase_category", cat);
            content = content.replace("knowledge/", "knowledge/9/");
	    post.setValue("sourcepath", content);
            archive.getSearcher("postdata").saveData(post);
        }
        
    }    

}

init();
