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
	String catalogid = "sitemanager/catalog";
	
	String clientform = context.getSessionValue("clientform");
	if (clientform != null) {
		context.putSessionValue("clientform", null);
	}
	else {
		context.putPageValue("errormsg","Invalid Submission. Please try <a href='./createsite.html'>again</a>.");
		return;
	}

	String name = context.getRequestParameter("name");
	String email = context.getRequestParameter("email");

	if (name != "" && email != "") {
		email = email.toLowerCase();
		String organization = context.getRequestParameter("organization");
		String instanceurl = context.getRequestParameter("organization_url");
		String organization_type = context.getRequestParameter("organization_type");
                String timezone = context.getRequestParameter("timezone");	
	
		//
		// String clientsubdomain = context.getRequestParameter("clientsubdomain");
		//
		
		Searcher clientsearcher = searcherManager.getSearcher(catalogid, "trial_clients");
		Data newclient = clientsearcher.createNewData();
		
		newclient.setName(name);
		
		//Validate Email
		String email_regex = /[_A-Za-z0-9-]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})/;
		if (email ==~ email_regex) {
			newclient.setProperty("clientemail",email);  
		}
		else {
			context.putPageValue("errormsg","Invalid Email.");
			info.log("Invalid Email ");
			return;
		}
		
		newclient.setProperty("organization",organization);
		newclient.setProperty("instanceurl",instanceurl);
		newclient.setProperty("organizationtype", organization_type);
                newclient.setProperty("timezone",timezone);
		//newclient.setProperty("datestart",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		
		DateStorageUtil dateStorageUtil = DateStorageUtil.getStorageUtil();
		newclient.setValue("datestart", new Date());
		newclient.setValue("dateend", dateStorageUtil.addDaysToDate(new Date(), 30));
		clientsearcher.saveData(newclient,null);

		//Create Valid URL
        String selected_url = instanceurl.toLowerCase();

		context.putPageValue("selected_url", selected_url);
                context.putPageValue("organization", organization);

		//Search Next Available Seat
		Searcher seatssearcher = searcherManager.getSearcher(catalogid, "trial_seats");
		SearchQuery scquery = seatssearcher.createSearchQuery();
		HitTracker seats = seatssearcher.search(scquery);
		//Collection hits = seatssearcher.query().all().not("clientid","*").search()
		Data seat = seatssearcher.query().match("seatstatus", "false").searchOne();

		if (seat) {
			//Use first available Seat
			//Data seat = hits.first();
			//Assign client to seat
			seat.setValue("clientid",newclient.id);
			seat.setValue("seatstatus","true");
			seatssearcher.saveData(seat, null);
			
			
			//Get Server Info
			Searcher servers = searcherManager.getSearcher(catalogid, "trial_servers");
			Data server = servers.query().match("id", seat.trial_servers).searchOne();
				
			//Call deploy script 
			// deploy_trial_client.sh SERVER SUBNET URL NODE
			try {
				List<String> command = new ArrayList<String>();
				//command.add(server.name); //server name
				command.add(server.sshname); //server name
				command.add(server.dockersubnet);  //server subnet
				command.add(selected_url);  //client url
				command.add(String.valueOf(seat.nodeid));  //client nodeid
				
				Exec exec = moduleManager.getBean("exec");
				ExecResult done = exec.runExec("setupclient", command);
				log.info("Exec: " + done.getStandardOut());
				
				Data client = clientsearcher.query().match("id", newclient.id).searchOne();
				
				client.setValue("trialstatus", "active");
				client.setValue("server_used", server.name);
				clientsearcher.saveData(client, null);
				
				
				context.putPageValue("userurl",selected_url + "." + server.serverurl);
				context.putPageValue("client_name", name);
				context.putPageValue("newuser", "admin");
				context.putPageValue("newpassword", "admin");
				
				context.putPageValue("from", email);
				context.putPageValue("subject", "New Activation - http://" + selected_url + "." +server.serverurl);
				sendEmail(context.getPageMap(),"help@entermediadb.org","/trialmanager/email/salesnotify.html");
				
				//Email Client
				context.putPageValue("from", 'help@entermediadb.org');
				context.putPageValue("subject", "Welcome to EnterMediaDB " + name);
				sendEmail(context.getPageMap(),email,"/trialmanager/email/businesswelcome.html");
			}
			catch(Exception e){
				 e.printStackTrace();
			}
			
		}
		else {
			log.info("No seats available");
			context.putPageValue("errormsg","No Demo sites available for now. Please contact EnterMedia support team.");
            //Send Email Notify No Seats
            context.putPageValue("from", email);
            context.putPageValue("subject", "No Seats Available for Trial Sites");
            sendEmail(context.getPageMap(),"help@entermediadb.org","/trialmanager/email/noseats.html");


		}
		
		
	}
}
   


protected void sendEmail(Map pageValues, String email, String templatePage){
	//send e-mail
	//Page template = getPageManager().getPage(templatePage);
	RequestUtils rutil = moduleManager.getBean("requestUtils");
	BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage,null, null);
	
	newcontext.putPageValues(pageValues);

	PostMail mail = (PostMail)moduleManager.getBean( "postMail");
	TemplateWebEmail mailer = mail.getTemplateWebEmail();
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	//mailer.setMessage(inOrder.get("sharenote"));
	//mailer.setWebPageContext(context);
	mailer.send();
	log.info("email sent to ${email}");
}

init();