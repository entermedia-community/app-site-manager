package org.entermedia.autofailover;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DnsRecord
{
	@JsonProperty("name")
	private String fieldName;
	
	@JsonProperty("type")
	private String fieldType;

	@JsonProperty("content")
	private String fieldContent;

	@JsonProperty("zone_id")
	private String fieldZoneId;

	@JsonProperty("regions")
	private Collection<String> fieldRegions;

	@JsonProperty("ttl")
	private Long fieldTtl;

	@JsonProperty("id")
	private Long fieldId;

	@JsonProperty("priority")
	private Long fieldPriority;

	@JsonProperty("updated_at")
	private String fieldUpdatedAt;
	
	@JsonProperty("created_at")
	private String fieldCreatedAt;

	@JsonProperty("parent_id")
	private int fieldParentId;

	@JsonProperty("system_record")
	private boolean fieldSystemRecord;
	
	public String getName()
	{
		return fieldName;
	}

	public void setName(String inName)
	{
		fieldName = inName;
	}

	public String getType()
	{
		return fieldType;
	}

	public void setType(String inType)
	{
		fieldType = inType;
	}

	public String getContent()
	{
		return fieldContent;
	}

	public void setContent(String inContent)
	{
		fieldContent = inContent;
	}

	public String getZoneId()
	{
		return fieldZoneId;
	}

	public void setZoneId(String inZoneId)
	{
		fieldZoneId = inZoneId;
	}

	public Collection<String> getRegions()
	{
		return fieldRegions;
	}

	public void setRegions(Collection<String> inRegions)
	{
		fieldRegions = inRegions;
	}

	public Long getTtl()
	{
		return fieldTtl;
	}

	public void setTtl(Long inTtl)
	{
		fieldTtl = inTtl;
	}

	public Long getId()
	{
		return fieldId;
	}

	public void setId(Long inId)
	{
		fieldId = inId;
	}

	public Long getPriority()
	{
		return fieldPriority;
	}

	public void setPriority(Long inPriority)
	{
		fieldPriority = inPriority;
	}

	public String getUpdatedAt()
	{
		return fieldUpdatedAt;
	}

	public void setUpdatedAt(String inUpdatedAt)
	{
		fieldUpdatedAt = inUpdatedAt;
	}

	public String getCreatedAt()
	{
		return fieldCreatedAt;
	}

	public void setCreatedAt(String inCreatedAt)
	{
		fieldCreatedAt = inCreatedAt;
	}

	public int getParentId()
	{
		return fieldParentId;
	}

	public void setParentId(int inParentId)
	{
		fieldParentId = inParentId;
	}

	public boolean getSystemRecord()
	{
		return fieldSystemRecord;
	}

	public void setSystemRecord(boolean inSystemRecord)
	{
		fieldSystemRecord = inSystemRecord;
	}

	public DnsRecord(String inName, String inType, String inContent, String inZoneId, Collection<String> inRegions, Long inTtl, Long inId, Long inPriority)
	{
		super();
		fieldName = inName;
		fieldType = inType;
		fieldContent = inContent;
		fieldZoneId = inZoneId;
		fieldRegions = inRegions;
		fieldTtl = inTtl;
		fieldId = inId;
		fieldPriority = inPriority;
	}
	
	public DnsRecord(String inName, String inType, String inContent, String inZoneId, Collection<String> inRegions, Long inTtl, Long inId, Long inPriority, String inUpdatedAt, String inCreatedAt, int inParentId, boolean inSystemRecord)
	{
		super();
		fieldName = inName;
		fieldType = inType;
		fieldContent = inContent;
		fieldZoneId = inZoneId;
		fieldRegions = inRegions;
		fieldTtl = inTtl;
		fieldId = inId;
		fieldPriority = inPriority;
		fieldUpdatedAt = inUpdatedAt;
		fieldCreatedAt = inCreatedAt;
		fieldParentId = inParentId;
		fieldSystemRecord = inSystemRecord;
	}

	public DnsRecord()
	{
		
	}
}
