import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
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
        String catalogid = "assets/catalog";

        //Search Clients with End Date = Today
        Searcher clientsearcher = searcherManager.getSearcher(catalogid, "trial_clients");

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, -15);


        Collection expiredClients = clientsearcher.query().exact("trialstatus","expired").and().before("dateend", now.getTime()).search();
        log.info("Found "+expiredClients.size()+" sites to delete.");

        expiredClients.each{
                //Get The Client
                Data client = clientsearcher.searchById(it.id);

                //Get Server Info
                Searcher servers = searcherManager.getSearcher(catalogid, "trial_servers");
                Data server = servers.query().exact("id", client.server).searchOne()
                if (server) {
                        log.info("Deleting client: "+client.name+", instance: "+client.instanceurl+" on server "+server.name);

                        List<String> command = new ArrayList<String>();
                        command.add(server.name); //server name
                        command.add(client.instanceurl);  //client url

                        Exec exec = moduleManager.getBean("exec");
                        ExecResult done = exec.runExec("removeclient", command);
                        //log.info("Exec: " + done.getStandardOut());

                        //Set Status Deleted to Client
                        client.setProperty("trialstatus","deleted");
                        client.setProperty("server","");
                        clientsearcher.saveData(client, null);
                }
        }
}

init();
