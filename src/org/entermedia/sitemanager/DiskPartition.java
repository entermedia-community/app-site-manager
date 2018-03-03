package org.entermedia.sitemanager;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiskPartition
{
	@JsonProperty("name")
	private String fieldName;
	
	@JsonProperty("totalcapacity")
	private Long fieldTotalCapacity;

	@JsonProperty("freepartitionspace")
	private Long fieldFreePartitionSpace;

	@JsonProperty("usablepartitionspace")
	private Long fieldUsablePartitionSpace;

	private boolean fieldIsOverloaded = false;
	
	public DiskPartition(String inName, Long inTotalCapacity, Long inFreePartitionSpace, Long inUsablePartitionSpace, boolean inIsOverloaded)
	{
		fieldName = inName;
		fieldTotalCapacity = inTotalCapacity;
		fieldFreePartitionSpace = inFreePartitionSpace;
		fieldUsablePartitionSpace = inUsablePartitionSpace;
		fieldIsOverloaded = inIsOverloaded;
	}

	public DiskPartition(Long inTotalCapacity, String inName, Long inUsablePartitionSpace, Long inFreePartitionSpace)
	{
		fieldName = inName;
		fieldTotalCapacity = inTotalCapacity;
		fieldFreePartitionSpace = inFreePartitionSpace;
		fieldUsablePartitionSpace = inUsablePartitionSpace;
	}

	public DiskPartition()
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

	public Long getTotalCapacity()
	{
		return fieldTotalCapacity;
	}

	public void setTotalCapacity(Long inTotalCapacity)
	{
		fieldTotalCapacity = inTotalCapacity;
	}

	public Long getFreePartitionSpace()
	{
		return fieldFreePartitionSpace;
	}

	public void setFreePartitionSpace(Long inFreePartitionSpace)
	{
		fieldFreePartitionSpace = inFreePartitionSpace;
	}

	public Long getUsablePartitionSpace()
	{
		return fieldUsablePartitionSpace;
	}

	public void setUsablePartitionSpace(Long inUsablePartitionSpace)
	{
		fieldUsablePartitionSpace = inUsablePartitionSpace;
	}

	public boolean isIsOverloaded()
	{
		return fieldIsOverloaded;
	}

	public void setIsOverloaded(boolean inIsOverloaded)
	{
		fieldIsOverloaded = inIsOverloaded;
	}

}
