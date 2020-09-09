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
		
		
		if (!expiredInstances.size()) {
			expiredInstances = instanceSearcher.query().exact("istrial", "true").exact("instance_status", "todelete").search();
		}

        log.info("Found "+expiredInstances.size()+" sites to delete.");

        for (Iterator instanceIterator = expiredInstances.iterator(); instanceIterator.hasNext();)
		{
                //Get The Client
                Data instance = instanceSearcher.loadData(instanceIterator.next());

				//Get Server Info
				Searcher serversSearcher = searcherManager.getSearcher(catalogid, "entermedia_servers");
				Data server = serversSearcher.searchById(instance.entermedia_servers);
                if (server) {
                        log.info("Deleting instance: "+instance.name+",  on server "+server.name);
						string instacename = instance.instanceprefix + "";
                        List<String> command = new ArrayList<String>();
						command.add(server.sshname); //server name
						command.add(instance.instancename);  // Docker id
						command.add(instance.instancenode);  // Docker Node

                        Exec exec = moduleManager.getBean("exec");
                        ExecResult done = exec.runExec("trialremove", command);
                        //log.info("Exec: " + done.getStandardOut());
						
						//Discount currentinstances on server
						if(instance.getValue("instance_status") == 'active') {
							server.setValue("currentinstances", server.getValue("currentinstances") - 1);
						}
						serversSearcher.saveData(server);
						

                        //Set Status Deleted to Client
                        instance.setProperty("instance_status","deleted");
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
