package org.openinstitute.finance;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.openedit.data.Searcher;

public class DateRange
{
	Date fieldStartDate;
	public Date getStartDate()
	{
		return fieldStartDate;
	}
	public void setStartDate(Date inStartDate)
	{
		fieldStartDate = inStartDate;
	}
	public Date getEndDate()
	{
		return fieldEndDate;
	}
	public void setEndDate(Date inEndDate)
	{
		fieldEndDate = inEndDate;
	}
	Date fieldEndDate;
	
	public void setMonthOfYear(int monthofyear)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, monthofyear);
		cal.set(Calendar.DAY_OF_MONTH,1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date start = cal.getTime();
		setStartDate(start);
		int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.add(Calendar.DAY_OF_MONTH,days);
		
		Date onemonth = cal.getTime();
		setEndDate(onemonth);
	}
	
}
