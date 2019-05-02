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

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, -15);


        Collection expiredClients = instanceSearcher.query().exact("istrial", "true").and().exact("instance_status","disabled").and().before("dateend", now.getTime()).search();
        log.info("Found "+ expiredClients.size() +" sites to delete.");

        expiredClients.each{
                //Get The Client
                Data instance = instanceSearcher.searchById(it.id);

                //Get Server Info
                Searcher servers = mediaArchive .getSearcher("entermedia_servers");
                Data server = servers.query().exact("id", instance.server).searchOne()
                if (server) {
                        log.info("Deleting client: "+instance.name+", instance: "+instance.instanceurl+" on server "+server.name);

                        List<String> command = new ArrayList<String>();
                        command.add(server.sshname); //server name
                        command.add(instance.instanceurl);  //client url

                        Exec exec = moduleManager.getBean("exec");
                        ExecResult done = exec.runExec("removeclient", command);
                        //log.info("Exec: " + done.getStandardOut());

                        //Set Status Deleted to Client
                        instance.setProperty("instance_status","deleted");
                        //client.setProperty("server","");
                        instanceSearcher.saveData(instance, null);
                }
        }
}

init();
