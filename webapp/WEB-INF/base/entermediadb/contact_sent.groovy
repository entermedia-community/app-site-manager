import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.entermediadb.email.GoogleCaptcha
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.data.BaseSearcher
import org.openedit.users.*
import org.openedit.hittracker.*
import org.openedit.util.DateStorageUtil
import org.openedit.util.RequestUtils

		
public void init()
{
		
	String catalogid = "entermediadb/catalog";
	String notifyemail = "help@entermediadb.org";

	//Send Email Notify No Seats
	context.putPageValue("from", "noreply@entermediadb.org");
	context.putPageValue("subject", "Message Received");
	context.putPageValue("form_name", context.getRequestParameter("name") );
	context.putPageValue("form_email", context.getRequestParameter("email") );
	context.putPageValue("form_message", context.getRequestParameter("message") );
	context.putPageValue("messagetime", new Date() );
	

	//logs
	String ipaddress = context.getRequest().getRemoteAddr();

	String senderinfo = "Url: "+context.getPageValue("siteroot");
	if (context.getPageValue("referringPage") != null) {
		senderinfo = senderinfo + " Refering page: "+context.getPageValue("referringPage");
	}
	if (context.getPageValue("page") != null) {
		senderinfo = senderinfo + " Page: "+context.getPageValue("page");
	}
	context.putPageValue("senderinfo",   senderinfo);
	senderinfo = senderinfo + " Ip: " + ipaddress;
	log.info(senderinfo);
	//log.info(context.getProperties());
	
	sendEmail(context.getPageMap(), notifyemail, "/entermediadb/contact_template.html");
}


//TODO: Make that table use the site (librarycollection)
protected void sendEmail(Map pageValues, String email, String templatePage){
	//Verify Captcha
	String usercaptcha = context.getRequestParameter("g-recaptcha-response");
	GoogleCaptcha captcha = (GoogleCaptcha)moduleManager.getBean("googleCaptcha");

	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	String captchaserverkey = mediaarchive.getCatalogSettingValue("googlecaptchaserverkey");
	
	captcha.setSecretKey(captchaserverkey);
	if(captcha.isValid(usercaptcha)) {
		//send e-mail
		RequestUtils rutil = moduleManager.getBean("requestUtils");
		BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage, null, null);
		newcontext.putPageValues(pageValues);
	
		PostMail mail = (PostMail)moduleManager.getBean( "postMail");
		TemplateWebEmail mailer = mail.getTemplateWebEmail();
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(email);
		mailer.send();
		context.putPageValue("error", null);
	}
	else {
		log.info("Contact Form - Invalid Captcha");
		context.putPageValue("error", "invalidcaptcha");
	}
	

}

init();



