package org.entermedia.sitemanager;


import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;

public class PaymentModule extends BaseMediaModule {
	
	private static final Log log = LogFactory.getLog(PaymentModule.class);

	protected SearcherManager fieldSearcherManager;
	protected HttpClient fieldHttpClient;
	protected StripePaymentProcessor fieldOrderProcessor;
	
	
	
	public StripePaymentProcessor getOrderProcessor()
	{
		if (fieldOrderProcessor == null)
		{
			fieldOrderProcessor = new StripePaymentProcessor();
			
		}

		return fieldOrderProcessor;
	}

	

	public HttpClient getHttpClient()
	{
		  RequestConfig globalConfig = RequestConfig.custom()
	                .setCookieSpec(CookieSpecs.DEFAULT)
	                .build();
	        HttpClient client = HttpClients.custom()
	                .setDefaultRequestConfig(globalConfig)
	                .build();
	        return client;
		
		
	}

	public void setHttpClient(HttpClient inHttpClient)
	{
		fieldHttpClient = inHttpClient;
	}
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	
	
	
	public void processPayment(WebPageRequest inReq){
		String token = inReq.getRequestParameter("stripeToken");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher payments = archive.getSearcher("transaction");
		Data payment = payments.createNewData();
		payments.updateData(inReq, inReq.getRequestParameters("field"), payment);
		
		getOrderProcessor().process(archive, inReq.getUser(), payment,  token);
		payment.setValue("paymentdate", new Date());
		String frequency = inReq.findValue("frequency");
		if(frequency != null && frequency != "") {
			Searcher plans = archive.getSearcher("paymentplan");
			Data plan = plans.createNewData();
			plan.setValue("userid", inReq.getUserName());
			plan.setValue("frequency", frequency);
			plan.setValue("amount", payment.getValue("totalprice"));
			plan.setValue("lastprocessed", new Date());
			plan.setValue("planstatus", "active");
			plans.saveData(plan);
			payment.setValue("paymentplan", plan.getId());
		}
		payments.saveData(payment);

		
	}
	
	
	
	
	public void processRecurringPayments(WebPageRequest inReq) {
		
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher payments = archive.getSearcher("transaction");
		Searcher plans = archive.getSearcher("paymentplan");

		Calendar now = Calendar.getInstance();
		now.add(now.DAY_OF_YEAR, -7);

		HitTracker toprocess = plans.query().before("lastprocessed", now.getTime()).exact("frequency", "weekly").exact("planstatus", "active").search();
		
		processTransactions(inReq, toprocess);		
		
		now = Calendar.getInstance();
		now.add(now.MONTH, -1);
		HitTracker monthly = plans.query().before("lastprocessed", now.getTime()).exact("frequency", "monthly").exact("planstatus", "active").search();
		
		processTransactions(inReq,monthly);

		
		now = Calendar.getInstance();
		now.add(now.YEAR, -1);
		HitTracker yearly = plans.query().before("lastprocessed", now.getTime()).exact("frequency", "yearly").exact("planstatus", "active").search();
		
		processTransactions(inReq,yearly);

//		
//		  <option value="">Once</option>
//		  <option value="weekly">Weekly</option>
//		  <option value="monthly">Monthly</option>
//		  <option value="yearly">Yearly</option>
//		
		
		
		
	}



	public void processTransactions(WebPageRequest inReq, HitTracker inYearly) {
		MediaArchive archive = getMediaArchive(inReq);
		Searcher payments = archive.getSearcher("transaction");
		
		Searcher paymentplans = archive.getSearcher("paymentplan");

		
		for (Iterator iterator = inYearly.iterator(); iterator.hasNext();) {
			Data paymentplan = (Data) iterator.next();
			String userid = paymentplan.get("userid");
			User user = archive.getUserManager().getUser(userid);
			Data payment = payments.createNewData();
			String amount = paymentplan.get("amount");
			if(amount == null) {
				continue;
			}
			payment.setValue("totalprice", amount);
			payment.setValue("paymentplan", paymentplan.getId());
			getOrderProcessor().process(archive, user, payment,  null);
			payments.saveData(payment);
			paymentplan.setValue("lastprocessed", new Date());
			paymentplans.saveData(paymentplan);
		}
		
	}
	
	
	
	
//	public void connectClient(WebPageRequest inReq){
//		String scope = inReq.getRequestParameter("scope");
//		String code = inReq.getRequestParameter("code");
//		if (scope == null || !scope.equals("read_write") || code == null){
//			inReq.putPageValue("error","Invalid request parameters");
//			return;
//		}
//		MediaArchive store = (MediaArchive) inReq.getPageValue("store");
//		if (store == null){
//			inReq.putPageValue("error","Store not defined");
//			return;
//		}
//		if (!"stripe".equals(store.get("gateway"))){
//			inReq.putPageValue("error","Store not configured for stripe");
//			return;
//		}
//		String clientsecret = null;
//		if (store.isProductionMode()){
//			clientsecret = store.get("secretkey");
//		} else {
//			clientsecret = store.get("testsecretkey");
//		}
//		if (clientsecret == null){
//			inReq.putPageValue("error","Stripe configuration error");
//			return;
//		}
//		HttpRequestBuilder builder = new HttpRequestBuilder();
//
//		HttpPost postMethod = null;
//		try
//		{
//			String fullpath = "https://connect.stripe.com/oauth/token";
//			
//			postMethod = new HttpPost(fullpath);
//			builder.addPart("code",code);
//			builder.addPart("client_secret",clientsecret);
//			builder.addPart("grant_type","authorization_code");
//			postMethod.setEntity(builder.build());
//			StatusLine line  = getHttpClient().execute(postMethod).getStatusLine();
//			if (line.getStatusCode() == 200)
//			{
//				//need to save response
//				String response = IOUtils.toString(postMethod.getEntity().getContent());
//				Type stringStringMap = new TypeToken<Map<String, String>>(){}.getType();
//				Map<String,String> map = new Gson().fromJson(response, stringStringMap);
//				if (map.containsKey("error") || !map.containsKey("access_token")){
//					inReq.putPageValue("error",map.get("error_description"));
//				} else{
//					String accesstoken = map.get("access_token");
//					String userid = map.get("stripe_user_id");
//					String publishkey = map.get("stripe_publishable_key");//js part
//					String refresh_token = map.get("refresh_token");
//					//persist in catalogsettings
//					MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
//					Searcher searcher = archive.getSearcher("catalogsettings");
//					List<Data> list = new ArrayList<Data>();
//					Data data = (Data) searcher.searchById("stripe_access_token");
//					if (data == null){
//						data = searcher.createNewData();
//						data.setId("stripe_access_token");
//						data.setName("Stripe Connect Access Token");
//					}
//					data.setProperty("value", accesstoken);
//					list.add(data);
//					Data data2 = (Data) searcher.searchById("stripe_user_id");
//					if (data2 == null){
//						data2 = searcher.createNewData();
//						data2.setId("stripe_user_id");
//						data2.setName("Stripe Connect User ID");
//					}
//					data2.setProperty("value", userid);
//					list.add(data2);
//					Data data3 = (Data) searcher.searchById("stripe_publishable_key");
//					if (data3 == null){
//						data3 = searcher.createNewData();
//						data3.setId("stripe_publishable_key");
//						data3.setName("Stripe Connect Publish Key");
//					}
//					data3.setProperty("value", publishkey);
//					list.add(data3);
//					//save to catalogsettings
//					
//					
//					Data data4 = (Data) searcher.searchById("stripe_refresh_token");
//					if (data4 == null){
//						data4 = searcher.createNewData();
//						data4.setId("stripe_refresh_token");
//						data4.setName("Stripe Refresh Token");
//					}
//					data4.setProperty("value", refresh_token);
//					list.add(data4);
//					
//					
//					
//					searcher.saveAllData(list, inReq.getUser());
//					
//					
//					inReq.putPageValue("accesstoken",accesstoken);
//					inReq.putPageValue("userid",userid);
//				}
//			} else {
//				inReq.putPageValue("error","Post Error: status code "+line.getStatusCode() );
//			}
//		}
//		catch (Exception e)
//		{
//			throw new OpenEditException(e.getMessage(), e);
//		}
//		
//		finally
//		{
//			if (postMethod != null)
//			{
//				try
//				{
//					postMethod.releaseConnection();
//				}
//				catch (Exception e){}
//			}
//		}
//		
//		generateTestKey(inReq);
//	}
//	
//	
//	
//	public void generateTestKey(WebPageRequest inReq){
////		curl -X POST https://connect.stripe.com/oauth/token \
////			  -d client_secret=sk_test_1UN7JHJIQxTA4wsBeXgyHj8u \
////			  -d refresh_token=REFRESH_TOKEN \
////			  -d grant_type=refresh_token
//		Store store = (Store) inReq.getPageValue("store");
//
//		
//		String clientsecret = store.get("testsecretkey");
//		
//		MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
//		Searcher searcher = archive.getSearcher("catalogsettings");
//		Data data4 = (Data) searcher.searchById("stripe_refresh_token");
//
//		HttpPost postMethod = null;
//		HttpRequestBuilder builder = new HttpRequestBuilder();
//
//		try
//		{
//			String fullpath = "https://connect.stripe.com/oauth/token";
//			
//			postMethod = new HttpPost(fullpath);
//			builder.addPart("refresh_token",data4.get("value"));
//			builder.addPart("client_secret",clientsecret);
//			builder.addPart("grant_type","refresh_token");
//			
//			int statusCode = getHttpClient().execute(postMethod).getStatusLine().getStatusCode();
//			if (statusCode == 200)
//			{
//				//need to save response
//				String response = IOUtils.toString(postMethod.getEntity().getContent());
//				Type stringStringMap = new TypeToken<Map<String, String>>(){}.getType();
//				Map<String,String> map = new Gson().fromJson(response, stringStringMap);
//				if (map.containsKey("error") || !map.containsKey("access_token")){
//					inReq.putPageValue("error",map.get("error_description"));
//				} else{
//					String key = map.get("stripe_publishable_key");
//					Data data = (Data) searcher.searchById("stripe_test_publishable_key");
//					if (data == null){
//						data = searcher.createNewData();
//						data.setId("stripe_test_publishable_key");
//						data.setName("Stripe Test Access Token");
//					}
//					data.setProperty("value", key);
//					searcher.saveData(data, inReq.getUser());
//				}
//			} else {
//				inReq.putPageValue("error","Post Error: status code "+statusCode);
//			}
//		}
//		catch (Exception e)
//		{
//			throw new OpenEditException(e.getMessage(), e);
//		}
//		
//		finally
//		{
//			if (postMethod != null)
//			{
//				try
//				{
//					postMethod.releaseConnection();
//				}
//				catch (Exception e){}
//			}
//		}
//	}
//	
//	public void loadTransactions(WebPageRequest inReq){
//		Store store = (Store) inReq.getPageValue("store");
//		//set application key
//		Data setting = null;
//		if(store.isProductionMode()){
//			 setting = getSearcherManager().getData(store.getCatalogId(), "catalogsettings", "stripe_access_token");
//			 log.info("loading catalogsettings from production mode");
//		} else{
//			 setting = getSearcherManager().getData(store.getCatalogId(), "catalogsettings", "stripe_test_access_token");
//			 log.info("loading catalogsettins from test mode");
//		}
//		if (setting!=null && setting.get("value")!=null){
//			Stripe.apiKey = setting.get("value");
//			log.info("set apikey from catalogsettings entry");
//		} else {
//			if(store.isProductionMode()){
//				Stripe.apiKey = store.get("secretkey");
//				log.info("set apikey using store's secret key in production mode");
//			} else{
//				Stripe.apiKey = store.get("testsecretkey");
//				log.info("set apikey using store's secret key in test mode");
//			}
//		}
//		String limit = inReq.getRequestParameter("limit");
//		if (limit == null || limit.isEmpty()){
//			limit = "10";
//		}
//		String after = inReq.getRequestParameter("after");
//		boolean fix = Boolean.parseBoolean(inReq.getRequestParameter("fix"));
//		log.info("<h4>Searching all Stripe charges, showing "+limit+", starting "+(after == null ? "at the beginning" : "after "+after)+"</h4>");
//		try{
//			List list = new ArrayList();
//			Map<String, Object> chargeParams = new HashMap<String, Object>();
//			chargeParams.put("limit", limit);//number of results to return
//			if (after!=null && !after.isEmpty()){
//				chargeParams.put("starting_after",after);
//			}
//			ChargeCollection col = Charge.all(chargeParams);
//			Iterator<Charge> itr = col.getData().iterator();
//			while(itr.hasNext()){
//				Charge charge = itr.next();
//				String orderid = charge.getDescription();
//				if (orderid == null || orderid.isEmpty()){
//					log.info("Unable to find an order id on charge "+charge.getId()+", skipping");
//					continue;
//				}
//				Order order = (Order) store.getOrderSearcher().searchById(orderid);
//				if (order == null){
//					log.info("Unable to load order "+orderid);
//					continue;
//				}
//				if ("stripe".equals(order.get("gateway")) == false){
//					order.setProperty("gateway","stripe");//update gateway
//				}
//				String transactionid = charge.getBalanceTransaction();
//				if (transactionid == null){
//					log.info("Order "+order.getId()+" balancetransaction not set, is refunded? "+charge.getRefunded());
//					continue;
//				}
//				updateValues(order,transactionid);
//				String stripefee = order.get("stripefee");
//				String profitshare = order.get("profitshare") == null ? "0" : order.get("profitshare");
//				String net = order.get("net");
//				//what was charged the client
//				Money stripetotal = new Money(stripefee).add(new Money(profitshare)).add(new Money(net));
//				//what is on the order
//				Money total = order.getTotalPrice();
//				//difference
//				Money delta = total.subtract(stripetotal);
//				if (!delta.isZero() && !delta.isNegative() && fix){
//					log.info("<span style='color:red'>Fixing "+order+", Correcting for "+delta+"</span>");
//					fixOrder(store, order,delta);
//					log.info("<br/>");
//				}
//				log.info("Order "+order.getId()+", Stripe Fee: "+stripefee+", Profit share: "+profitshare+", Net: "+net+", Stripe Total: "+stripetotal+", Order Total: "+order.getTotalPrice());
//				list.add(order);
//			}
//			log.info("<hr><strong>Saving changes to orders</strong>");
//			store.getOrderSearcher().saveAllData(list, null);
//			///print out last
//			int size = col.getData().size();
//			if (size == 0){
//				log.info("<hr><strong>Finished</strong>");
//			} else {
//				Charge last = col.getData().get(size - 1);
//				log.info("<hr>Last Charge ID: <strong>"+last.getId()+"</strong>");
//			}
//		} catch (Exception e){
//			log.error(e.getMessage(),e);
//		}
//	}
//	
//	protected void updateValues(Order inOrder, String inTransaction){
//		try{
//			BalanceTransaction balance = BalanceTransaction.retrieve(inTransaction);
//			List<Fee> details = balance.getFeeDetails();
//			for (Iterator<Fee> iterator = details.iterator(); iterator.hasNext();)
//			{
//				Fee fee = iterator.next();
//				//have to account for div by zero errors
//				//could we find a fee in this list that has a value of zero?
//				float moneyval = 0;
//				if (fee.getAmount().intValue() != 0){
//					moneyval = (float) fee.getAmount() / 100;
//				}
//				Money money = new Money(String.valueOf(moneyval));
//				if("stripe_fee".equals(fee.getType())){
//					inOrder.setProperty("stripefee", money.toShortString());
//				} else if("application_fee".equals(fee.getType())){
//					inOrder.setProperty("profitshare", money.toShortString());
//				}
//			}
//			float net = (float) balance.getNet() / 100;
//			Money money = new Money(String.valueOf(net));
//			inOrder.setProperty("net",money.toShortString());
//		}catch (Exception e){
//			log.error(e.getMessage(),e);
//		}
//	}
//	
//	protected void fixOrder(Store inStore, Order inOrder, Money inMoney){
//		//correct order by adding an adjustment
//		//associate it with first non-coupon item on the order
//		//account for changes to tax as well
//		
//		//check order first: no shipping, one item on cart
//		if (inOrder.getShippingMethod() != null || inOrder.getItems().size() > 1 || !inOrder.getAdjustments().isEmpty() || inOrder.getTaxes().size() > 1){
//			log.info("Unable to fix order "+inOrder.getId()+": has more than one item, or has a shipping method, or has adjustments, or has more than one tax rate, skipping");
//			return;
//		}
//		Money subtotal = inOrder.getSubTotal();
//		Money tax = inOrder.getTax();
//		Money total = inOrder.getTotalPrice();
//		
//		CartItem item = (CartItem) inOrder.getItems().get(0);
//		Customer customer = inStore.createCustomer();
//		customer.setTaxRates(new ArrayList<TaxRate>());
//		DiscountAdjustment adjustment = new DiscountAdjustment();
//		adjustment.setProductId(item.getProduct().getId());
//		adjustment.setInventoryItemId(item.getSku());
//		if (tax.isZero()){
//			adjustment.setDiscount(inMoney);//no tax on the order so just add corrected amount
//		} else {
//			Map<TaxRate,Money> map = inOrder.getTaxes();
//			TaxRate rate = null;
//			Iterator<TaxRate> keys = map.keySet().iterator();
//			while (keys.hasNext()){
//				rate = keys.next();
//				customer.getTaxRates().add(rate);//should have only one TaxRate
//			}
//			//original price
//			Fraction fraction = rate.getFraction();
//			Fraction subfact = fraction.add(1);
//			Money discount = inMoney.divide(subfact);
//			adjustment.setDiscount(discount);
//		}
//		//create a new cart and place customer / adjustments on it
//		Cart cart = new Cart();
//		cart.setCustomer(customer);
//		cart.addAdjustment(adjustment);
//		cart.addItem(item);
//		
//		Money newsubtotal = cart.getSubTotal();
//		Money newtax = cart.getTotalTax();
//		Money newtotal = cart.getTotalPrice();
//		
//		Money delta = total.subtract(inMoney).subtract(newtotal);
//		if (delta.doubleValue() < 0.005){
//			//add adjustments and new tallies
//			inOrder.getAdjustments().add(adjustment);
//			inOrder.setSubTotal(newsubtotal);
//			inOrder.setTotalTax(newtax);
//			inOrder.setTotalPrice(newtotal);
//			log.info("<span style='color:blue;'>Original Total: "+total+" New Total: "+inOrder.getTotalPrice()+", Difference: "+delta+"</span>");
//		} else {
//			log.info("<span style='color:red;'>Unable to update order "+inOrder.getId()+"</span>");
//		}
//	}
}
