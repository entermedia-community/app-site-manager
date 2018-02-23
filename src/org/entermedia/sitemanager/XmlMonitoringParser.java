package org.entermedia.sitemanager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openedit.OpenEditException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlMonitoringParser
{
	private String heap;
	private String heappercent;
	private String memoryfree;
	private String memorytotal;
	private String cpu;
	private String diskfree;
	private String disktotal;
	private String diskavailable;
	private String stat;
	private URL url;
	
	
	public XmlMonitoringParser(URL url)
	{
		this.url = url;
		parseXML();
	}

	public void parseXML(){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			InputStream inputStream = url.openStream();
			InputSource is = new InputSource(new InputStreamReader(inputStream, "UTF-8"));
			is.setEncoding("UTF-8");
			
			
			
			Document doc = dBuilder.parse(is);
			if (doc.hasChildNodes()){
				NodeList nl = doc.getChildNodes();
				processNodes(nl);
			}
		} catch (Exception e){
			e.printStackTrace();
			throw new OpenEditException(e.getMessage(),e);
		}
	}
	
	private void processNodes(NodeList nl){
		for (int i=0; i < nl.getLength(); i++){
			Node node = nl.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE || node.getNodeName()==null)
				continue;
			if (node.getNodeName().equalsIgnoreCase("heapused")){
				this.heap = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("heapusedpercent")){
				this.heappercent = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("loadaverage")){
				this.cpu = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("servermemoryfree")){
				this.memoryfree = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("servermemorytotal")){
				this.memorytotal = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("diskfree")){
				this.diskfree = node.getFirstChild().getTextContent();
			} else if (node.getNodeName().equalsIgnoreCase("disktotal")){
				this.disktotal = node.getFirstChild().getTextContent();
			} else if (node.getAttributes().getNamedItem("stat") != null ){
				this.stat = node.getAttributes().getNamedItem("stat").getNodeValue();
			} else if (node.getNodeName().equalsIgnoreCase("diskavailable")){
				this.diskavailable = node.getFirstChild().getTextContent();
			} 
			if (node.hasChildNodes()){
				processNodes(node.getChildNodes());
			}
		}
	}

	public String getHeap()
	{
		return heap;
	}

	public void setHeap(String heap)
	{
		this.heap = heap;
	}

	public String getHeappercent()
	{
		return heappercent;
	}

	public void setHeappercent(String heappercent)
	{
		this.heappercent = heappercent;
	}

	public String getMemoryfree()
	{
		return memoryfree;
	}

	public void setMemoryfree(String memoryfree)
	{
		this.memoryfree = memoryfree;
	}

	public String getMemorytotal()
	{
		return memorytotal;
	}

	public void setMemorytotal(String memorytotal)
	{
		this.memorytotal = memorytotal;
	}

	public String getCpu()
	{
		return cpu;
	}

	public void setCpu(String cpu)
	{
		this.cpu = cpu;
	}

	public String getDiskfree()
	{
		return diskfree;
	}

	public void setDiskfree(String diskfree)
	{
		this.diskfree = diskfree;
	}

	public String getDisktotal()
	{
		return disktotal;
	}

	public void setDisktotal(String disktotal)
	{
		this.disktotal = disktotal;
	}

	public URL getUrl()
	{
		return url;
	}

	public void setUrl(URL url)
	{
		this.url = url;
	}

	public String getStat()
	{
		return stat;
	}

	public void setStat(String stat)
	{
		this.stat = stat;
	}

	public String getDiskavailable()
	{
		return diskavailable;
	}

	public void setDiskavailable(String diskavailable)
	{
		this.diskavailable = diskavailable;
	}
	
}
