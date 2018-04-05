package org.entermedia.serverstats;

import java.util.ArrayList;

public class ServerStats
{
	private Object swapSize;
	private Object swapFree;
	private Object memoryfree;
	private Object memorytotal;
	private Object cpu;
	private Object processCPU;
	private Object totalassets;

	public ServerStats()
	{

	}

	public void build(ArrayList<ServerStat> inStats)
	{
		for (ServerStat stat : inStats)
		{
			if (stat.getName().equals("getTotalSwapSpaceSize"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setSwapSize(stat.getErrorMsg());
				}
				else
				{
					setSwapSize(stat.getValue());
				}
			}
			else if (stat.getName().equals("getFreeSwapSpaceSize"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setSwapFree(stat.getErrorMsg());
				}
				else
				{
					setSwapFree(stat.getValue());
				}
			}
			//			else if (stat.getName().equals("getProcessCpuTime"))
			//			{
			//				setProcessCPU(stat.getValue());
			//			}
			else if (stat.getName().equals("getFreePhysicalMemorySize"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setMemoryfree(stat.getErrorMsg());
				}
				else
				{
					setMemoryfree(stat.getValue());
				}
			}
			else if (stat.getName().equals("getTotalPhysicalMemorySize"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setMemorytotal(stat.getErrorMsg());
				}
				else
				{
					setMemorytotal(stat.getValue());
				}
			}
			//			else if (stat.getName().equals("getOpenFileDescriptorCount"))
			//			{
			//				
			//			}
			//			else if (stat.getName().equals("getMaxFileDescriptorCount"))
			//			{
			//				
			//			}
			else if (stat.getName().equals("getSystemCpuLoad"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setCpu(stat.getErrorMsg());
				}
				else
				{
					setCpu(stat.getValue());
				}
			}
			else if (stat.getName().equals("getProcessCpuLoad"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setProcessCPU(stat.getErrorMsg());
				}
				else
				{
					setProcessCPU(stat.getValue());
				}
			}
			else if (stat.getName().equals("totalassets"))
			{
				if (stat.getErrorMsg() != null && !stat.getErrorMsg().isEmpty())
				{
					setTotalassets(stat.getErrorMsg());
				}
				else
				{
					setTotalassets(stat.getValue());
				}
			}
		}

	}

	public Object getSwapSize()
	{
		return swapSize;
	}

	public void setSwapSize(Object inSwapSize)
	{
		swapSize = inSwapSize;
	}

	public Object getSwapFree()
	{
		return swapFree;
	}

	public void setSwapFree(Object inSwapFree)
	{
		swapFree = inSwapFree;
	}

	public Object getMemoryfree()
	{
		return memoryfree;
	}

	public void setMemoryfree(Object inMemoryfree)
	{
		memoryfree = inMemoryfree;
	}

	public Object getMemorytotal()
	{
		return memorytotal;
	}

	public void setMemorytotal(Object inMemorytotal)
	{
		memorytotal = inMemorytotal;
	}

	public Object getCpu()
	{
		return cpu;
	}

	public void setCpu(Object inCpu)
	{
		cpu = inCpu;
	}

	public Object getProcessCPU()
	{
		return processCPU;
	}

	public void setProcessCPU(Object inProcessCPU)
	{
		processCPU = inProcessCPU;
	}

	public Object getTotalassets()
	{
		return totalassets;
	}

	public void setTotalassets(Object inTotalassets)
	{
		totalassets = inTotalassets;
	}

}
