package org.entermedia.sitemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.money.Money;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.XmlUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Fee;

public class StripePaymentProcessor {

	private static final Log log = LogFactory.getLog(StripePaymentProcessor.class);
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;

	private HttpResponse<String> httpPostRequest(MediaArchive inArchive, URI uri) throws IOException, InterruptedException {
		log.info(uri);
		boolean productionmode = inArchive.isCatalogSettingTrue("productionmode");
		String apiKey = productionmode ? inArchive.getCatalogSettingValue("stripe_access_token")
				: inArchive.getCatalogSettingValue("stripe_test_access_token");
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Authorization", "Bearer " + apiKey)
				.POST(HttpRequest.BodyPublishers.ofString("")).build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		log.info(response.body());
		return response;
	}
	
	private HttpResponse<String> httpGetRequest(MediaArchive inArchive, URI uri) throws IOException, InterruptedException {
		boolean productionmode = inArchive.isCatalogSettingTrue("productionmode");
		String apiKey = productionmode ? inArchive.getCatalogSettingValue("stripe_access_token")
				: inArchive.getCatalogSettingValue("stripe_test_access_token");
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Authorization", "Bearer " + apiKey)
				.GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		log.info(response.body());
		return response;
	}
	
	private String getItemId(HttpResponse<String> response) throws JsonParseException, JsonMappingException, IOException {
		if (response.statusCode() != 200) {
			return "";
		}		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(response.body(), Map.class);
		return (String) map.get("id");
	}

	protected boolean createCharge(MediaArchive inArchive, User inUser, Data payment, String source) throws IOException, InterruptedException, URISyntaxException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/prices");
		Money totalprice = new Money(payment.get("totalprice"));
		String amountstring = totalprice.toShortString().replace(".", "").replace("$", "").replace(",", "");
		String currency = inArchive.getCatalogSettingValue("currency") != null
				? inArchive.getCatalogSettingValue("currency")
				: "usd";
		URI uri = new URIBuilder(http.getURI()).addParameter("amount", amountstring).addParameter("currency", currency)
				.addParameter("source", source)
				.addParameter("description", "My First Test Charge (created for API docs)").build();
		HttpResponse<String> response = httpPostRequest(inArchive, uri);
		// TODO: log this somewhere
		if (response.statusCode() != 200) {
			return false;
		}
		return true;
	}
	
	protected String createProduct(MediaArchive inArchive, String productId) throws URISyntaxException, IOException, InterruptedException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/products");
		Data product = inArchive.getProductById(productId);		
		
		URI uri = new URIBuilder(http.getURI())
				.addParameter("name", product.getName())
				.addParameter("description", (String)product.getValue("productdescription"))
				.build();
		HttpResponse<String> response = httpPostRequest(inArchive, uri);
		return getItemId(response);
	}
	
	protected String createPrice(MediaArchive inArchive, String amount, String intervalCount, String productId) throws URISyntaxException, IOException, InterruptedException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/prices");
		String currency = inArchive.getCatalogSettingValue("currency") != null ? inArchive.getCatalogSettingValue("currency")
				: "usd";
		URI uri = new URIBuilder(http.getURI())
				.addParameter("unit_amount", amount)
				.addParameter("currency", currency)
				.addParameter("recurring[interval]", "month")
				.addParameter("recurring[interval_count]", intervalCount)
				.addParameter("product", productId).build();				
		HttpResponse<String> response = httpPostRequest(inArchive, uri);
		return getItemId(response);
	}
	
	protected String getCustomerId(MediaArchive inArchive, String email, String source) throws URISyntaxException, IOException, InterruptedException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/customers");
		URI uri = new URIBuilder(http.getURI())
				.addParameter("email", email).build();
		HttpResponse<String> response = httpGetRequest(inArchive, uri);
		if (response.statusCode() != 200) {
			return "";
		}
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(response.body(), Map.class);
		ArrayList<Map<String, Object>> users = (ArrayList) map.get("data");
		String userId = "";
		for (Map<String, Object> x : users) {
			if (x.get("email").equals(email)) {
				userId = (String)x.get("id");
			}
		};
		
		//TODO if source is different, update source?
		return userId;
	}

	protected String createCustomer2(MediaArchive inArchive, User inUser, String source) throws URISyntaxException, IOException, InterruptedException {
		// TODO: check if users exists, probably by email, if it exists just return customer's id
		if (source.isEmpty()) {
			return "";
		}
		String email =  (String) inUser.getValue("email");
		String emailExists = getCustomerId(inArchive, email, source);
		if (emailExists != null && !emailExists.isEmpty()) {
			return emailExists;
		}
		HttpPost http = new HttpPost("https://api.stripe.com/v1/customers");
		URI uri = new URIBuilder(http.getURI())
				.addParameter("description", inUser.getId())
				.addParameter("email", email)
				.addParameter("source", source).build();				
		HttpResponse<String> response = httpPostRequest(inArchive, uri);
		return getItemId(response);
	}
	
	protected String createSubscription(MediaArchive inArchive, User inUser, String customer, String price) throws URISyntaxException, IOException, InterruptedException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/subscriptions");
		URI uri = new URIBuilder(http.getURI())
				.addParameter("customer", customer)
				.addParameter("items[0][price]", price).build();				
		HttpResponse<String> response = httpPostRequest(inArchive, uri);
		return getItemId(response);
	}
	
	protected ArrayList<Map<String, Object>> getSubscriptions(MediaArchive inArchive, String customer) throws URISyntaxException, IOException, InterruptedException {
		HttpPost http = new HttpPost("https://api.stripe.com/v1/subscriptions");
		URI uri = new URIBuilder(http.getURI())
				.addParameter("customer", customer).build();
		HttpResponse<String> response = httpGetRequest(inArchive, uri);
		
		if (response.statusCode() != 200) {
			return null;
		}		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(response.body(), Map.class);
		return (ArrayList<Map<String, Object>>) map.get("data");
	}

	protected boolean process(MediaArchive inArchive, User inUser, Data payment, String inToken) {
		log.info("processing order with Stripe");

		Map<String, Object> chargeParams = new HashMap<String, Object>();
		Money totalprice = new Money(payment.get("totalprice"));
		// stripe connect: use access_token generated by oauth in place of
		// secretkey; also define application fee (application_fee parameter)
		boolean forcetestmode = Boolean.parseBoolean(payment.get("forcetestmode"));
		boolean productionmode = inArchive.isCatalogSettingTrue("productionmode");
		if (productionmode && !forcetestmode) {
			log.info("Passing in prod mode");
			Stripe.apiKey = inArchive.getCatalogSettingValue("stripe_access_token");
		} else {
			log.info("Passing in test mode: " + String.valueOf(forcetestmode));
			Stripe.apiKey = inArchive.getCatalogSettingValue("stripe_test_access_token");
		}
		String amountstring = totalprice.toShortString().replace(".", "").replace("$", "").replace(",", "");
		chargeParams.put("amount", amountstring);
		String currency = inArchive.getCatalogSettingValue("currency");
		if (currency == null) {
			currency = "usd";
		}
		chargeParams.put("currency", currency);
		String descriptor = inArchive.getCatalogSettingValue("statement_descriptor");
		if (descriptor != null) {
			chargeParams.put("statement_descriptor", descriptor);
		}
		Map<String, String> initialMetadata = new HashMap<String, String>();
		populateMetadata(payment, inUser, initialMetadata);
		chargeParams.put("description", payment.getId());
		chargeParams.put("metadata", initialMetadata);
		try {
			String customerid = inUser.get("stripeid"); // This might fail with the admin user
			// No such customer: cus_C9JdWVqvhQ16Uq; a similar object exists in test mode,
			// but a live mode key was used to make this request.
			if (customerid == null) {
				customerid = createCustomer(inArchive, inUser, inToken);
			}
			chargeParams.put("customer", customerid); // obtained via https://stripe.com/docs/saving-cards

			Charge c = null;
			try {
				c = Charge.create(chargeParams);
			} catch (com.stripe.exception.InvalidRequestException ex) {
				// : No such customer: cus_GyDBm8HUKk4U6p
				if (ex.getMessage().startsWith("No such customer")) // Customer was deleted or this is a new account
				{
					customerid = createCustomer(inArchive, inUser, inToken);
					chargeParams.put("customer", customerid); // obtained via js
					c = Charge.create(chargeParams);
				} else {
					throw new OpenEditException(ex);
				}
			}

			if (c.getPaid()) {
				String balancetransaction = c.getBalanceTransaction();
				BalanceTransaction balance = BalanceTransaction.retrieve(balancetransaction);
				long fee = balance.getFee();
				float moneyval = (float) fee / 100;
				payment.setProperty("fee", String.valueOf(moneyval));
				List<Fee> details = balance.getFeeDetails();
				for (Iterator<Fee> iterator = details.iterator(); iterator.hasNext();) {
					Fee fee2 = iterator.next();
					float feeval = (float) fee2.getAmount() / 100;
					if ("stripe_fee".equals(fee2.getType())) {
						payment.setProperty("stripefee", String.valueOf(feeval));
					}
					if ("application_fee".equals(fee2.getType())) {
						payment.setProperty("profitshare", String.valueOf(feeval));
					}
				}
				payment.setProperty("balancetransaction", balancetransaction);
				float net = (float) balance.getNet() / 100;
				payment.setProperty("net", String.valueOf(net));
				payment.setProperty("stripechargeid", c.getId());
				return true;
			} else {
				return false;
			}
		}

		catch (Exception e) {
			throw new OpenEditException(e);
		}

	}

	protected String createCustomer(MediaArchive inArchive, User inUser, String inToken) throws AuthenticationException,
			InvalidRequestException, APIConnectionException, CardException, APIException {
		String customerid;
		Map<String, Object> customerParams = new HashMap<String, Object>();
		customerParams.put("email", inUser.getEmail());
		customerParams.put("source", inToken);

		Customer customer = Customer.create(customerParams);
		customerid = customer.getId();
		inUser.setValue("stripeid", customer.getId());
		inArchive.getUserManager().saveUser(inUser);
		return customerid;
	}

//	@Override
//	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder, Refund inRefund) throws StoreException
//	{
//		if (!requiresValidation(inStore, inOrder)) {
//			return;
//		}
//		log.info("refunding order with Stripe");
//		if(inOrder.get("stripetoken") == null || inOrder.get("stripechargeid")==null){
//			log.error("cannot find stripetoken, aborting");
//			return;
//		}
//		String chargeId = inOrder.get("stripechargeid");
//		//set application key
//		boolean forcetestmode = inOrder.getBoolean("forcetestmode");
//		Data setting = null;
//		if(inStore.isProductionMode()){
//			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_access_token");
//		} else{
//			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_test_access_token");
//		}
//		if (setting!=null && setting.get("value")!=null){
//			Stripe.apiKey = setting.get("value");
//		} else {
//			//check if an administrator has ordered test mode transaction
//			if(inStore.isProductionMode() && !forcetestmode){
//				Stripe.apiKey = inStore.get("secretkey");//livesecretkey or secretkey
//			} else{
//				Stripe.apiKey = inStore.get("testsecretkey");
//			}
//		}
//		try{
//			//load the charge
//			Charge c = Charge.retrieve(chargeId);
//			if (c.getRefunded()){//this is true if fully refunded
//				inRefund.setSuccess(false);
//				inRefund.setMessage("Order has already been refunded");
//
//				inRefund.setDate(new Date());
//			} else {
//				Integer total = c.getAmount();
//				Integer totalrefunded = c.getAmountRefunded();
//				Integer refundamount = Integer.parseInt(inRefund.getTotalAmount().toShortString().replace(".","").replace(",",""));
//				if(refundamount > total){
//					refundamount = total;
//				}
//				Map<String, Object> params = new HashMap<String, Object>();
//				params.put("amount", String.valueOf(refundamount));
//				com.stripe.model.Refund refund = c.getRefunds().create(params);
//				inRefund.setSuccess(true);
//				inRefund.setProperty("refundedby" , inContext.getUserName());
//
//				inRefund.setAuthorizationCode(refund.getId());
//				inRefund.setTransactionId(refund.getBalanceTransaction());
//				inRefund.setDate(new Date());
//				//calculate whether application fees should be handled
//				//need to fix this logic:
//				//handle application fees work only when we have one item on the cart 
//				//and when the store is not setup to handle profit shares
//				handleApplicationFees(chargeId,inRefund);
//				
//				//OLD!!!
////				Charge refundedCharge = c.refund();//refunds the full amount
////				if (refundedCharge.getRefunded()){
////					ChargeRefundCollection refunds = refundedCharge.getRefunds();
////					com.stripe.model.Refund refund = refunds.getData().get(0);
////					inRefund.setSuccess(true);
////					inRefund.setAuthorizationCode(refund.getId());
////					inRefund.setTransactionId(refund.getBalanceTransaction());
////					inRefund.setDate(new Date());
////					//client was refunded at this point, but
////					//partners have not been
////					//handle this at the end
////					handleApplicationFees(chargeId,inRefund);
////					
////				} else {
////					inRefund.setSuccess(false);
////					inRefund.setMessage("Order could not be refunded");
////					inRefund.setDate(new Date());
////				}
//			}
//		}catch (Exception e){
//			inRefund.setSuccess(false);
//			inRefund.setMessage("An error occurred while processing your transaction.");
//			e.printStackTrace();
//			throw new StoreException(e);
//		}
//	}

//	protected void handleApplicationFees(String inChargeId, Refund inRefund){
//		try{
//			Map<String, Object> params = new HashMap<String, Object>();
//			params.put("charge",inChargeId);
//			ApplicationFeeCollection collection = ApplicationFee.all(params);
//			List<ApplicationFee> fees  = collection.getData();
//			if (fees.size() > 0){
//				//refund all fees
//				StringBuilder buf = new StringBuilder();
//				for (ApplicationFee fee:fees){
//					//here we will probably have to refund partial amounts
//					if (!fee.getRefunded()){
//						ApplicationFee refundedFee = fee.refund();
//						if (!refundedFee.getRefunded()){
//							buf.append("ID:").append(fee.getId()).append(" ");
//						}
//					}
//				}
//				if (buf.toString().isEmpty()){
//					inRefund.setMessage("All application fees were refunded");
//				} else {
//					inRefund.setMessage("Unable to refund all application fees, "+buf.toString().trim());
//				}
//			} else {
//				inRefund.setMessage("No application fees were found on this order");
//			}
//		}catch (Exception e){
//			log.error(e.getMessage(), e);
//		}
//	}

	protected void populateMetadata(Data inOrder, User inUser, Map<String, String> inMetadata) {
		inMetadata.put("firstname", inUser.getFirstName());
		inMetadata.put("lastname", inUser.getLastName());
		inMetadata.put("email", inUser.getEmail());

//		Address billing = inOrder.getCustomer().getBillingAddress();
//		inMetadata.put("billingaddress",billing.toString());
//		Address shipping = inOrder.getCustomer().getShippingAddress();
//		inMetadata.put("shippingaddress",shipping.toString());
//		Iterator<?> itr = inOrder.getItems().iterator();
//		for(int i=1; itr.hasNext(); i++){
//			CartItem item = (CartItem) itr.next();
//			String sku = item.getSku();
//			String name = item.getName();
//			Money price = item.getYourPrice();
//			StringBuilder buf = new StringBuilder();
//			buf.append(sku).append(": ");
//			if (Coupon.isCoupon(item)){
//				buf.append("Coupon - ");
//			}
//			buf.append(name).append(" ").append(price.toShortString());
//			inMetadata.put("cartitem-"+i, buf.toString());
//		}
	}

//	public Money calculateFee(MediaArchive inStore, Data inOrder){
//		Money totalFee = new Money("0");
//		@SuppressWarnings("unchecked")
//		Iterator<CartItem> itr = inOrder.getItems().iterator();
//		while(itr.hasNext()){
//			CartItem item = itr.next();
//			Product product = item.getProduct();
//			if (product.isCoupon()){
//				continue;
//			}
//			String fee = product.get("partnershipfee");
//			String type = product.get("partnershipfeetype");
//			if (fee!=null && type!=null){
//				if (type.equals("flatrate")){
//					Money money = new Money(fee);
//					if (money.isNegative() || money.isZero()){
//						continue;
//					}
//					totalFee = totalFee.add(money);
//				} else if (type.equals("percentage")){
//					Money itemprice = item.getTotalPrice();
//					double rate = Double.parseDouble(fee);
//					if (rate < 0.0d || rate > 1.0d){
//						continue;
//					}
//					Money money = itemprice.multiply(new Fraction(rate));
//					totalFee = totalFee.add(money);
//				}
//			}
//		}
//		if (totalFee.isZero() && inStore.get("fee_structure")!=null){
//			String fee_structure = inStore.get("fee_structure");
//			double rate = Double.parseDouble(fee_structure);
//			totalFee = new Money(rate);
//			if (rate < 1.0d){
//				totalFee = inOrder.getSubTotal().multiply(new Fraction(rate));
//			}
//		}
//		String fee = inOrder.get("fee");//transaction fee
//		double totalFeed = totalFee.doubleValue();
//		if (fee!=null && fee.isEmpty()==false){
//			Money transfee = new Money(fee);
//			if (transfee.isZero() == false && totalFee.isZero() == false){
//				Money charged = inOrder.getSubTotal(); //this is what we sent to stripe
//				double chargedd = charged.doubleValue();
//				double ratio = totalFeed/chargedd;
//				
//				
//				
//				transfee = transfee.multiply(new Fraction(ratio));//divide by 2  Not correct - need to divide by the ratio of the share
//				totalFee = totalFee.subtract(transfee);
//			}
//		}
//		inOrder.setProperty("profitshare", Double.toString(totalFee.doubleValue()));
//		return totalFee;
//	}
//	

}
