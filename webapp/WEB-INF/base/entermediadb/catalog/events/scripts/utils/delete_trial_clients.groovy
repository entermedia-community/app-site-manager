package utils;

import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.entermediadb.asset.MediaArchive
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil

import org.openedit.BaseWebPageRequest
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init()
{
         MediaArchive mediaArchive = context.getPageValue("mediaarchive");

        //Search Clients with End Date = Today
        Searcher instanceSearcher = mediaArchive .getSearcher("entermedia_instances");

        Calendar limit = Calendar.getInstance();
        limit.add(Calendar.DAY_OF_YEAR, -15);

        Collection expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status","disabled").and().before("dateend", limit.getTime()).search();
        log.info("Found "+expiredInstances.size()+" sites to delete.");

        for (Iterator instanceIterator = expiredInstances.iterator(); instanceIterator.hasNext();)
		{
                //Get The Client
                Data instance = instanceSearcher.loadData(instanceIterator.next());

				//Get Server Info
                Searcher servers = mediaArchive .getSearcher("entermedia_servers");
                Data server = servers.query().exact("id", instance.entermedia_servers).searchOne();
                if (server) {
                        log.info("Deleting instance: "+instance.name+",  on server "+server.name);

                        List<String> command = new ArrayList<String>();
                        command.add(server.sshname); //server name
                        command.add(instance.instanceprefix);  //client url

                        Exec exec = moduleManager.getBean("exec");
                        ExecResult done = exec.runExec("removeclient", command);
                        //log.info("Exec: " + done.getStandardOut());

                        //Set Status Deleted to Client
                        instance.setProperty("instance_status","deleted");
                        //client.setProperty("server","");
                        instanceSearcher.saveData(instance, null);
						
						//Delete Monitoring
						Collection todelete = new ArrayList();
						Searcher monitorsearcher = mediaArchive.getSearcher("entermedia_instances_monitor");
						Data instancemonitor = monitorsearcher.query().match("instanceid", instance.id).searchOne();
						if (instancemonitor) {
							//instancemonitor.setValue("monitoringenable","false");
							//monitorsearcher.saveData(instancemonitor, null);
							todelete.add(instancemonitor);
						}
						monitorsearcher.deleteAll(todelete, null);
                }
        }
}

init();
