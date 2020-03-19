import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.data.BaseSearcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils

public void init()
{

	String catalogid = "entermediadb/catalog";
	String notifyemail = "help@entermediadb.org";
	String notifyemailcc = "help@entermediadb.org";
	
	String collectionid = context.getRequestParameter("collectionid");  //collectionid
	String requester = context.getRequestParameter("followeruser.value");
	
	String isanAgent = context.getRequestParameter("ontheteam.value");
	if (isanAgent != null && isanAgent== 'true') {
		//Do not send notification if requester is an Agent.
		return;
	}
	
	Data collection = mediaarchive.getData("librarycollection",collectionid);

	//Send Email to the Collection Owner?
	if (collection.owner != null) 
	{
		Data theowner = mediaarchive.getUser(collection.owner);
		if (theowner != null && theowner.email != null) 
		{
			notifyemail = theowner.email;
		}
	}
	
	if (!notifyemail.equals(notifyemailcc))
	{
		notifyemail = notifyemail+","+notifyemailcc;
	}
	
	Data therequester = mediaarchive.getUser(requester);
	
	context.putPageValue("requester", therequester.name);
	context.putPageValue("project", collection.name);
	String appid = context.findValue("sitelink");
	String link = "collective/sites/index.html?collectionid="+collectionid;
	context.putPageValue("link", link);
	
	context.putPageValue("from", "noreply@entermediadb.org");
	context.putPageValue("subject", "EM Client Portal: Access to Team Request");
	
	//log.info("Sending Email to:"+therequester.name+" at:"+notifyemail+" about:"+collection.name+" link:"+link);
	
	sendEmail(context.getPageMap(), notifyemail, "/entermediadb/app/collective/channel/subscribers/notifyemail.html");
}



protected void sendEmail(Map pageValues, String email, String templatePage){
	//send e-mail
	//Page template = getPageManager().getPage(templatePage);
	RequestUtils rutil = moduleManager.getBean("requestUtils");
	BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage, null, null);
	
	newcontext.putPageValues(pageValues);

	PostMail mail = (PostMail)moduleManager.getBean("postMail");
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


