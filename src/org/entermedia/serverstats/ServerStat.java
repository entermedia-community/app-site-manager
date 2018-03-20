package org.entermedia.serverstats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerStat
{
	@JsonProperty("name")
	private String fieldName;
	@JsonProperty("value")
	private Object fieldValue;
	@JsonProperty("error")
	private String errorMsg;
	
	public ServerStat(String inName, Object inValue, String inErrorMsg)
	{
		fieldName = inName;
		fieldValue = inValue;
		errorMsg = inErrorMsg;
	}
	
	public ServerStat()
	{
		
	}

	public String getName()
	{
		return fieldName;
	}

	public void setName(String inName)
	{
		fieldName = inName;
	}

	public Object getValue()
	{
		return fieldValue;
	}

	public void setValue(Object inValue)
	{
		fieldValue = inValue;
	}

	public String getErrorMsg()
	{
		return errorMsg;
	}

	public void setErrorMsg(String inErrorMsg)
	{
		errorMsg = inErrorMsg;
	}


}
