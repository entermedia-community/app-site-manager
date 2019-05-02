package org.entermedia.invoice;

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

public class InvoiceManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(InvoiceManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected DecimalFormat df = new DecimalFormat("#.00"); 


	public int getUnpaidInvoiceNumber(String inCollectionId)
	{
		int res = 0;
		Collection<Data> clientinvoices = getMediaArchive().query("clientinvoice").match("clientinvoicestatus", InvoiceStatus.UNPAID).exact("librarycollection", inCollectionId).search();

		if (clientinvoices != null)
		{
			res = clientinvoices.size();
		}
		return res;
	}

	public int getPaidInvoiceNumber(String inCollectionId)
	{
		int res = 0;
		Collection<Data> clientinvoices = getMediaArchive().query("clientinvoice").match("status", InvoiceStatus.PAID).exact("librarycollection", inCollectionId).search();

		if (clientinvoices != null)
		{
			res = clientinvoices.size();
		}
		return res;
	}

	public int getCancelledInvoiceNumber(String inCollectionId)
	{
		int res = 0;
		Collection<Data> clientinvoices = getMediaArchive().query("clientinvoice").match("status", InvoiceStatus.CANCELLED).exact("librarycollection", inCollectionId).search();
		
		if (clientinvoices != null)
		{
			res = clientinvoices.size();
		}
		return res;
	}

	public int getRefundedInvoiceNumber(String inCollectionId)
	{
		int res = 0;
		Collection<Data> clientinvoices = getMediaArchive().query("clientinvoice").match("status", InvoiceStatus.REFUNDED).exact("librarycollection", inCollectionId).search();

		if (clientinvoices != null)
		{
			res = clientinvoices.size();
		}
		return res;
	}


	
	public String getAccountBalance(String inCollectionId)
	{
		Searcher invoiceSearcher = getMediaArchive().getSearcher("clientinvoice");
		Collection<Data> clientinvoices = invoiceSearcher.query().match("clientinvoicestatus", InvoiceStatus.UNPAID).exact("librarycollection", inCollectionId).search();
		double amount = 0;

		for (Data it : clientinvoices)
		{
			MultiValued real = (MultiValued) invoiceSearcher.loadData(it);

			try
			{
				amount += (double)real.getValue("amount");
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
