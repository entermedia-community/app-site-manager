import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.GoogleCaptcha
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.util.RequestUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

		
public void init()
{
		
	String catalogid = "entermediadb/catalog";
	String notifyemail = "help@entermediadb.org";

	//For logs
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
	//--
	
	//Verify Captcha
	String usercaptcha = context.getRequestParameter("g-recaptcha-response");
	GoogleCaptcha captcha = (GoogleCaptcha)moduleManager.getBean("googleCaptcha");

	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	String captchaserverkey = mediaarchive.getCatalogSettingValue("googlecaptchaserverkey");
	
	captcha.setSecretKey(captchaserverkey);
	if(!captcha.isValid(usercaptcha)) {
		log.info("Contact Form Error - Invalid Captcha - ${senderinfo}");
		context.putPageValue("error", "invalidcaptch");
		return;
	}
	
	context.putPageValue("form" , "noreply@entermediadb.org");
	context.putPageValue("subject", "Message Received");
	context.putPageValue("form_email", context.getRequestParameter("email") );
	
	//Verify Spam Links
	String form_name = context.getRequestParameter("name");
	boolean spamlinks_name = containsURL(form_name);
	
	String form_message = context.getRequestParameter("message");
	boolean spamlinks_message = containsURL(form_message);
	if (spamlinks_name || spamlinks_message) {
		log.info("Contact Form Error - Spam Detected - ${senderinfo}");
		context.putPageValue("error", "spamdetected");
		return;
	}
	
	context.putPageValue("form_name",  form_name);
	context.putPageValue("form_message", form_message );
	context.putPageValue("messagetime", new Date() );
	
	log.info("Contact Form - Message validated ${senderinfo}");
	//log.info(context.getProperties());
	
	sendEmail(context.getPageMap(), notifyemail, "/entermediadb/contact_template.html");
}


//TODO: Make that table use the site (librarycollection)
protected void sendEmail(Map pageValues, String email, String templatePage){
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
		log.info("Contact Form - Message Sent");
		context.putPageValue("error", null);

}

private boolean containsURL(String content){
	String REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	Pattern p = Pattern.compile(REGEX,Pattern.CASE_INSENSITIVE);
	Matcher m = p.matcher(content);
	return m.find();
}

init();



