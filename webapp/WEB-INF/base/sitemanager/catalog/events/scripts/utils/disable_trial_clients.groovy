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
        //String catalogid = "sitemanager/catalog";

        MediaArchive mediaArchive = context.getPageValue("mediaarchive");

        //Search Clients with End Date = Today
        Searcher clientsearcher = mediaArchive.getSearcher("trial_clients");

        Date today = new Date();
        Collection expiredClients = clientsearcher.query().exact("trialstatus","active").and().before("dateend", today).search();
        log.info("Found "+expiredClients.size()+" sites expired.");
        expiredClients.each{
                //Get The Client
                Data client = clientsearcher.searchById(it.id);

                log.info("Disabling: "+client.name+" -> "+client.dateend);

                //Search Seat Info
                Searcher seatssearcher = mediaArchive.getSearcher("trial_seats");
                SearchQuery scquery = seatssearcher.createSearchQuery();
                HitTracker seats = seatssearcher.search(scquery);
                Data seat = seatssearcher.query().match("clientid", client.id).searchOne();
                if (seat) {
                                //Get Server Info
                                Searcher servers = mediaArchive.getSearcher("trial_servers");
                                Data server = servers.query().exact("id", seat.trial_servers).searchOne()
                                if (server) {
                                        List<String> command = new ArrayList<String>();
                                        command.add(server.name); //server name
                                        command.add(client.instanceurl);  //client url
                                        command.add(String.valueOf(seat.nodeid));  //client nodeid

                                        Exec exec = moduleManager.getBean("exec");
                                        ExecResult done = exec.runExec("disableclient", command);
                                        //log.info("Exec: " + done.getStandardOut());

                                        seat.setValue("clientid","");
                                        seat.setValue("seatstatus","false");
                                        seatssearcher.saveData(seat, null);


                                        //Set Status Expired to Client
                                        client.setProperty("trialstatus","expired");
                                        client.setProperty("server", seat.trial_servers);
                                        clientsearcher.saveData(client, null);

                                        //Email Client
                                        context.putPageValue("client_name", client.name);
                                				context.putPageValue("from", 'help@entermediadb.org');
                                				context.putPageValue("subject", "EnterMediaDB Instance Expired");
                                        sendEmail(context.getPageMap(),email,"/trialmanager/email/expired.html");
                                }
                }


        }
}

init();
