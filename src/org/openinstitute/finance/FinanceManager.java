package org.openinstitute.finance;

import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;

public class FinanceManager  implements CatalogEnabled
{
	
	protected ModuleManager fieldModuleManager;
	protected String fieldCatalogId;

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


	//get expenses from expenses db
	
	
}
