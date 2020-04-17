package org.entermedia.transactions;

import java.text.DecimalFormat;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

public class TransactionManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(TransactionManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected DecimalFormat df = new DecimalFormat("#.00"); 

	
	public String getTransactionTotal(String inCollectionId)
	{
		Searcher invoiceSearcher = getMediaArchive().getSearcher("transaction");
		Collection<Data> clientinvoices = invoiceSearcher.query().exact("collectionid", inCollectionId).search();
		double amount = 0;

		for (Data it : clientinvoices)
		{
			MultiValued real = (MultiValued) invoiceSearcher.loadData(it);

			try
			{
				amount = amount + real.getFloat("totalprice");
			}
			catch (Exception e)
			{
				log.error("Can't retrieve/parse invoice " + real.get("name") + " amount", e);
			}
		}
		
		return df.format(amount);
		
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
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

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

}
