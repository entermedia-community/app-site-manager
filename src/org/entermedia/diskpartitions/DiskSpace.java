package org.entermedia.diskpartitions;

import java.util.ArrayList;

public class DiskSpace
{
	ArrayList<DiskPartition> partitions;

	public DiskSpace(ArrayList<DiskPartition> inPartitions)
	{
		partitions = inPartitions;
	}
	
	public boolean isOnePartitionOverloaded()
	{
		for (DiskPartition partition : getPartitions())
		{
			if (partition.isIsOverloaded())
			{
				return true;
			}
		}
		return false;
	}

	public ArrayList<DiskPartition> getPartitions()
	{
		return partitions;
	}

	public void setPartitions(ArrayList<DiskPartition> inPartitions)
	{
		partitions = inPartitions;
	}
}
