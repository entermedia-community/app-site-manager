package org.openinstitute.finance;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.entermedia.transactions.TransactionManager;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FinanceManager  implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(TransactionManager.class);
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;
	protected String fieldCatalogId;
	protected DecimalFormat df = new DecimalFormat("#.00"); 

	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	
	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}


	//get expenses from expenses db
	
	public ArrayList<Map<String, Object>>   getTotalIncomesByDateRange(String inCollectionId, DateRange inDateRange)
	{
		Searcher incomesSearcher = getMediaArchive().getSearcher("transaction");
		Collection<Data> incomes = incomesSearcher.query().exact("collectionid", inCollectionId).search();
		//inDateRange
		
		ArrayList<Map<String, Object>>  incomesbyCurrency = null;
		
		//TODO: Group by currency and return array/Collection?

		return incomesbyCurrency;
		
	}
	
	public ArrayList<Map<String, Object>>   getIncomeTypesByDateRange(String inCollectionId, DateRange inDateRange)
	{
		Searcher incomesSearcher = getMediaArchive().getSearcher("transaction");
		return null;
	}
	
	public ArrayList<Map<String, Object>>   getTotalExpensesByDateRange(String inCollectionId, DateRange inDateRange)
	{
		Searcher expenseSearcher = getMediaArchive().getSearcher("expense");
		//TODO: Group by currency and return array/Collection?
		
		return null;
	}
	
	public String getTotalExpenseByCurrency(String inCollectionId, String inCurrencyId, DateRange inDateRange) 
	{
		Searcher expenseSearcher = getMediaArchive().getSearcher("expense");
		return "";
	}
	
	

	
}
