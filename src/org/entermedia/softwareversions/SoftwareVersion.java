package org.entermedia.softwareversions;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SoftwareVersion
{
	@JsonProperty("name")
	private String name;
	@JsonProperty("version")
	private String version;

	public SoftwareVersion()
	{
	}

	public SoftwareVersion(String inName, String inVersion)
	{
		name = inName;
		version = inVersion;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String inName)
	{
		name = inName;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String inVersion)
	{
		version = inVersion;
	}

}
