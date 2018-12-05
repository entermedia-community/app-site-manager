import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.page.Page
import org.openedit.page.manage.PageManager
import org.openedit.util.DateStorageUtil
import org.openedit.util.Exec
import org.openedit.util.ExecResult

public void init(){

	MediaArchive archive = context.getPageValue("mediaarchive");
	Exec exec = archive.getModuleManager().getBean("exec");
	List<String> com = new ArrayList<String>();
	com.add("-c");
	PageManager pm = archive.getModuleManager().getBean("pageManager");
	Page config = pm.getPage("/${archive.catalogId}/configuration/rsnapshot.conf");
	String fullpath = config.getContentItem().getAbsolutePath();
	com.add(fullpath);
	com.add("hourly");
	ExecResult execresult = exec.runExec("rsnapshot", com, true);
	boolean ok = execresult.isRunOk();
	Searcher backupsearcher = archive.getSearcher("backup");
	Data backup = archive.getData("backup", "backup");
	
	if(backup == null){
		backup = archive.getSearcher("backup").createNewData();
		backup.setId("backup");
	}
	backup.setProperty("time", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	
	Searcher backuphistory = archive.getSearcher("backuphistory");
	Data history = backuphistory.createNewData();
	history.setId(backuphistory.nextId());
	history.setProperty("time", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	history.setProperty("backup", "backup");
	
	if(ok){
		//problems
		log.info("Backup Completed " );
		backup.setProperty("status", "Backup Completed");
		history.setProperty("status", "Backup Completed");
	}else{
			
		backup.setProperty("status", "Backup Failed");
		history.setProperty("status", "Backup Completed");
		history.setProperty("note", execresult.getStandardError() );
		
	
		
	}

	archive.getSearcher("backup").saveData(backup, null);
	backuphistory.saveData(history, null);
	
	}
init();

