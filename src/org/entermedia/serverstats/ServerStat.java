package org.entermedia.serverstats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerStat
{
	@JsonProperty("name")
	private String fieldName;
	@JsonProperty("value")
	private Object fieldValue;
	@JsonProperty("error")
	private Object fieldError;
	
	public ServerStat(String inName, Object inValue)
	{
		fieldName = inName;
		fieldValue = inValue;
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

	public Object getError()
	{
		return fieldError;
	}

	public void setError(Object inError)
	{
		fieldError = inError;
	}

}
