package org.openinstitute.finance;

import java.util.Calendar;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class FinanceModule extends BaseMediaModule
{

	public void loadDateRange(WebPageRequest inReq)
	{
		DateRange range = (DateRange)inReq.getSessionValue("daterange");
		String newrange = inReq.getRequestParameter("daterangestart");
		if( newrange != null)
		{
			//parse dates
		}
		if( range == null)
		{
			range = new DateRange();
			range.setMonthOfYear(Calendar.getInstance().get(Calendar.MONTH));
		}
		inReq.putSessionValue("daterange", range);
		
	}
	public void loadFinanceManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		FinanceManager manager = (FinanceManager)getModuleManager().getBean(catalogid, "financeManager");
		inReq.putPageValue("financeManager",manager);
	}
}
