package org.entermedia.diskpartitions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiskPartition
{
	private static final Log log = LogFactory.getLog(DiskPartition.class);

	
	@JsonProperty("name")
	private String fieldName;
	
	@JsonProperty("totalcapacity")
	private Long fieldTotalCapacity;

	@JsonProperty("freepartitionspace")
	private Long fieldFreePartitionSpace;

	@JsonProperty("usablepartitionspace")
	private Long fieldUsablePartitionSpace;
	
	private Double fieldUsagePercent;

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

	private void setUsagePercentage()
	{
		if (fieldFreePartitionSpace == null || fieldTotalCapacity == null)
			throw new OpenEditException("Can't retrieve instance's server hardware usage");
		fieldFreePartitionSpace = fieldTotalCapacity - fieldFreePartitionSpace;
		
		fieldUsagePercent = (double)100.0 * fieldFreePartitionSpace / fieldTotalCapacity;
	}
	
	public boolean isOverloaded(int maxUsage)
	{
		try
		{
			setUsagePercentage();
		}
		catch (Exception e)
		{
			log.error("Disk overload check failed ", e);
			return false;
		}
		if (fieldUsagePercent >= maxUsage)
		{
			fieldIsOverloaded = true;
			return true;
		}
		fieldIsOverloaded = false;
		return false;
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

	public Double getUsagePercent()
	{
		return fieldUsagePercent;
	}

	public void setUsagePercent(Double inUsagePercent)
	{
		fieldUsagePercent = inUsagePercent;
	}

}
