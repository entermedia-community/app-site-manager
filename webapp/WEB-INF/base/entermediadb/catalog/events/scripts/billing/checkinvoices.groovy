package billing;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.User

public void init() {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher productSearcher = mediaArchive .getSearcher("collectiveproduct");

	generateRecurringInvoices(mediaArchive, productSearcher);
	sendInvoiceNotifications(mediaArchive);
}

public void generateRecurringInvoices(MediaArchive mediaArchive, Searcher productSearcher) {
	int daysToExpire = 7; // invoice will expire in (days)
	Calendar today = Calendar.getInstance();
	Calendar due = Calendar.getInstance();
	due.add(Calendar.DAY_OF_YEAR, -5); // make invoice 5 days before next bill date

	Collection pendingProducts = productSearcher.query()
			.exact("recurring","true")
			.exact("billingstatus", "active")
			.before("nextbillon", today.getTime())
			.after("nextbillon", due.getTime()).search();

	log.info("Checking invoice for " + pendingProducts.size() + " products");

	for (Iterator productIterator = pendingProducts.iterator(); productIterator.hasNext();) {
		Data product = productSearcher.loadData(productIterator.next());

		Date nextBillOn = product.getValue("nextbillon");
		Date lastbilldate = product.getValue("lastgeneratedinvoicedate");
		if (lastbilldate < nextBillOn) { // otherwise assume it's already created
			Searcher invoiceSearcher = mediaArchive.getSearcher("collectiveinvoice");
			Data invoice = invoiceSearcher.createNewData();
			invoice.setName(product.getName());

			Calendar invoiceDue = Calendar.getInstance();
			invoiceDue.add(Calendar.DAY_OF_YEAR, daysToExpire);
			HashMap<String,Object> productItem = new HashMap<String,Object>();
			productItem.put("productid", product.getValue("id"));
			productItem.put("productquantity", 1 );
			productItem.put("productprice", product.getValue("productprice"));
			Collection items = new ArrayList();
			items.add(productItem);
			invoice.setValue("productlist", items);
			invoice.setValue("paymentstatus", "invoiced");
			invoice.setValue("collectionid", product.getValue("collectionid"));
			invoice.setValue("owner", product.getValue("owner"));
			invoice.setValue("totalprice", product.getValue("productprice"));
			invoice.setValue("duedate", invoiceDue.getTime());
			invoice.setValue("invoicedescription", product.getValue("productdescription"));
			invoice.setValue("notificationsent", today.getTime());
			invoice.setValue("createdon", "false");
			invoiceSearcher.saveData(invoice);

			int recurrentCount = product.getValue("recurringperiod")
			int currentMonth = nextBillOn.getMonth();
			nextBillOn.setMonth(currentMonth + recurrentCount);
			product.setValue("nextbillon", nextBillOn);
			product.setValue("lastgeneratedinvoicedate", today.getTime());
			productSearcher.saveData(product);
		}
	}
}

public void sendInvoiceNotifications(MediaArchive mediaArchive) {
	Searcher invoiceSearcher = mediaArchive .getSearcher("collectiveinvoice");
	Collection pendingNotificationInvoices = invoiceSearcher.query()
			.exact("notificationsent","false").search();

	log.info("Sending Notification for " + pendingNotificationInvoices.size() + " invoices");
	for (Iterator invoiceIterator = pendingNotificationInvoices.iterator(); invoiceIterator.hasNext();) {
		Data invoice = invoiceSearcher.loadData(invoiceIterator.next());

		Searcher teamSearcher = mediaArchive .getSearcher("librarycollectionusers");
		Collection invoiceMembers = teamSearcher.query()
				.exact("collectionid", invoice.getValue("collectionid"))
				.exact("isbillingcontact", "true")
				.search();

		log.info("Sending Notification for " + invoiceMembers.size() + " members");
		for (Iterator teamIterator = invoiceMembers.iterator(); teamIterator.hasNext();) {
			Data member = teamSearcher.loadData(teamIterator.next());
			User contact = mediaArchive.getUser(member.getValue("followeruser"));

			if (contact != null) {
				String email = contact.getValue("email");
				log.info("sending email to: " + email);
				if (email) {
					sendInvoice(mediaArchive, contact, invoice);
				}
				//TODO send email?
			}
		}
		invoice.setValue("notificationsent", "true");
		// invoiceSearcher.saveData(invoice);
	}
}

public void sendInvoice(MediaArchive mediaArchive, User contact, Data invoice) {
	String appid = mediaArchive.getCatalogSettingValue("events_billing_notify_invoice_appid");
	String template = "/" + appid + "/theme/emails/send-invoice-event.html";
	
	String site = mediaArchive.getCatalogSettingValue("siteroot");
	if (!site) {
		site = mediaArchive.getCatalogSettingValue("cdn_prefix");
	}
	
	String supportUrl = site + "/entermediadb/app/collective/services/index.html?collectionid=" + invoice.getValue("collectionid");
	String actionUrl = site + "/entermediadb/app/collective/community/index.html?collectionid=" + invoice.getValue("collectionid");
	
	WebEmail templateEmail = mediaArchive.createSystemEmail(contact, template);
	templateEmail.setSubject("Invoice");

	Map objects = new HashMap();
	objects.put("followeruser", contact);
	objects.put("mediaarchive", mediaArchive);
	objects.put("invoice", invoice);
	objects.put("supporturl", supportUrl);
	objects.put("actionurl", actionUrl);

	templateEmail.send(objects);
}

public void sendOverdue(MediaArchive mediaArchive, User contact, Data invoice) {
	String appid = mediaArchive.getCatalogSettingValue("events_billing_notify_invoice_appid");
	String template = "/" + appid + "/theme/emails/send-invoice-overdue-event.html";
	WebEmail templateEmail = mediaArchive.createSystemEmail(contact, template);
	templateEmail.setSubject("Invoice");
	Map objects = new HashMap();
	objects.put("followeruser", contact);
	objects.put("mediaarchive", mediaArchive);
	objects.put("invoice", invoice);
	templateEmail.send(objects);
}

public void sendPaymentReceived(MediaArchive mediaArchive, User contact, Data invoice) {
	String appid = mediaArchive.getCatalogSettingValue("events_billing_notify_invoice_appid");
	String template = "/" + appid + "/theme/emails/send-invoice-payment-received-event.html";
	WebEmail templateEmail = mediaArchive.createSystemEmail(contact, template);
	templateEmail.setSubject("Invoice");
	Map objects = new HashMap();
	objects.put("followeruser", contact);
	objects.put("mediaarchive", mediaArchive);
	objects.put("invoice", invoice);
	templateEmail.send(objects);
}

//overdue
//invoiced
//paid


//public void CheckRecurringEmptyBills(Searcher productSearcher) {
//	Collection	pendingProducts = productSearcher.query().exact("recurring","true").exact("billingstatus", "active").search();
//	for (Iterator productIterator =	pendingProducts.iterator(); productIterator.hasNext();) {
//		Data product = productSearcher.loadData(productIterator.next());
//
//		Calendar due = product.getValue("nextbillon"); int count =
//		product.getValue("recurringperiod"); if (due == null && count != null) {
//			log.info("Found a misconfigured product");
//		}
//	}
//}



//public void SetLastLoginDefault(MediaArchive mediaArchive, Searcher  instanceSearcher) {
//	Collection instances =  instanceSearcher.query().exact("istrial",  "true").exact("instance_status","active").search(); // TODO: refine query.  just search for lastlogin == null or empty
//	for (Iterator instanceIterator =instances.iterator(); instanceIterator.hasNext();) {
//		Data instance =		mediaArchive.getSearcher("entermedia_instances").loadData(instanceIterator.		next());
//		if (instance.lastlogin == null) {
//			instance.setValue("lastlogin", new			Date());
//			instanceSearcher.saveData(instance, null);
//		}
//	}
//}



init();
