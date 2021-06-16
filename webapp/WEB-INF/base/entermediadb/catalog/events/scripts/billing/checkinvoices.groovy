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

	Searcher invoiceSearcher = mediaArchive .getSearcher("collectiveinvoice");
	sendInvoiceNotifications(mediaArchive, invoiceSearcher);
	sendInvoiceOverdueNotifications(mediaArchive, invoiceSearcher);
	sendInvoicePaidNotifications(mediaArchive, invoiceSearcher);
}

private void generateRecurringInvoices(MediaArchive mediaArchive, Searcher productSearcher) {
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
			invoice.setValue("notificationsent", "false");
			invoice.setValue("createdon", today.getTime());
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

private void sendInvoiceNotifications(MediaArchive mediaArchive, Searcher invoiceSearcher) {
	Collection pendingNotificationInvoices = invoiceSearcher.query()
			.exact("notificationsent","false").search();

	log.info("Sending Notification for " + pendingNotificationInvoices.size() + " invoices");
	invoiceContactIterate(mediaArchive, invoiceSearcher, pendingNotificationInvoices, "notificationsent");
}

private void sendInvoiceOverdueNotifications(MediaArchive mediaArchive, Searcher invoiceSearcher) {
	Calendar today = Calendar.getInstance();
	Collection pendingNotificationInvoices = invoiceSearcher.query()
			.before("duedate", today.getTime())
			.exact("notificationoverduesent", "false")
			.exact("paymentstatus","invoiced").search();

	log.info("Found " + pendingNotificationInvoices.size() + " overdue invoices");
	invoiceContactIterate(mediaArchive, invoiceSearcher, pendingNotificationInvoices, "notificationoverduesent");
}

private void sendInvoicePaidNotifications(MediaArchive mediaArchive, Searcher invoiceSearcher) {
	Calendar today = Calendar.getInstance();
	Collection pendingNotificationInvoices = invoiceSearcher.query()
			.before("duedate", today.getTime())
			.exact("notificationpaidsent", "false")
			.exact("paymentstatus","paid").search();

	log.info("Found " + pendingNotificationInvoices.size() + " paid invoices");
	invoiceContactIterate(mediaArchive, invoiceSearcher, pendingNotificationInvoices, "notificationpaidsent");
}

private void invoiceContactIterate(MediaArchive mediaArchive, Searcher invoiceSearcher, Collection invoices, String iteratorType) {
	for (Iterator invoiceIterator = invoices.iterator(); invoiceIterator.hasNext();) {
		Data invoice = invoiceSearcher.loadData(invoiceIterator.next());

		Searcher teamSearcher = mediaArchive .getSearcher("librarycollectionusers");
		Collection invoiceMembers = teamSearcher.query()
				.exact("collectionid", invoice.getValue("collectionid"))
				.exact("isbillingcontact", "true")
				.search();

		for (Iterator teamIterator = invoiceMembers.iterator(); teamIterator.hasNext();) {
			Data member = teamSearcher.loadData(teamIterator.next());
			User contact = mediaArchive.getUser(member.getValue("followeruser"));

			if (contact != null) {
				String email = contact.getValue("email");
				if (email) {
					switch (iteratorType) {
						case "notificationsent":
							sendEmail(mediaArchive, contact, invoice, "Invoice", "send-invoice-event.html");
							break;
						case "notificationoverduesent":
							sendEmail(mediaArchive, contact, invoice, "Overdue Invoice", "send-overdue-invoice-event.html");
							break;
						case "notificationpaidsent":
							sendEmail(mediaArchive, contact, invoice, "Payment Received", "send-paid-invoice-event.html");
							break;
					}
				}
			}
		}

		invoice.setValue(iteratorType, "true");
		invoiceSearcher.saveData(invoice);
	}
}

private void sendEmail(MediaArchive mediaArchive, User contact, Data invoice, String subject, String htmlTemplate) {
	String appid = mediaArchive.getCatalogSettingValue("events_billing_notify_invoice_appid");
	String template = "/" + appid + "/theme/emails/" + htmlTemplate;

	String site = mediaArchive.getCatalogSettingValue("siteroot");
	if (!site) {
		site = mediaArchive.getCatalogSettingValue("cdn_prefix");
	}

	String supportUrl = site + "/entermediadb/app/collective/services/index.html?collectionid=" + invoice.getValue("collectionid");
	String actionUrl = site + "/entermediadb/app/collective/community/index.html?collectionid=" + invoice.getValue("collectionid");
	WebEmail templateEmail = mediaArchive.createSystemEmail(contact, template);
	templateEmail.setSubject(subject);
	Map objects = new HashMap();
	objects.put("followeruser", contact);
	objects.put("mediaarchive", mediaArchive);
	objects.put("invoice", invoice);
	objects.put("supporturl", supportUrl);
	objects.put("actionurl", actionUrl);
	templateEmail.send(objects);
}

init();
