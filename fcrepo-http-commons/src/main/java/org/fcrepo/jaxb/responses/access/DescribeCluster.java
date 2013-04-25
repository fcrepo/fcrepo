package org.fcrepo.jaxb.responses.access;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "clusterConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class DescribeCluster {
	
	public final Map<String, String> cacheModeToDisplay = new HashMap<String, String>();
	
	@XmlElement
	public String cacheMode;
	
	@XmlElement
	public String clusterName;
	
	@XmlElement
	public String nodeAddress;
	
	@XmlElement
	public String physicalAddress;
	
	@XmlElement
	public int nodeView;
	
	@XmlElement
	public int clusterSize;
	
	@XmlElement
	public String clusterMembers;
	
	public DescribeCluster() {
		this.cacheModeToDisplay.put("DIST_ASYNC", "Asynchronous Distribution");
		this.cacheModeToDisplay.put("DIST_SYNC", "Synchronous Distribution");
		this.cacheModeToDisplay.put("INVALIDATION_ASYNC", "Data invalidated asynchronously");
		this.cacheModeToDisplay.put("INVALIDATION_SYNC", "Data invalidated synchronously");
		this.cacheModeToDisplay.put("LOCAL", "Data is not replicated");
		this.cacheModeToDisplay.put("REPL_ASYNC", "Data replicated asynchronously");
		this.cacheModeToDisplay.put("REPL_SYNC", "Data replicated synchronously");
	}
	
	//Getters and setters mainly used for velocity template access
	public String getCacheMode() {
		return cacheMode;
	}

	public void setCacheMode(String cacheMode) {
		this.cacheMode = cacheModeToDisplay.get(cacheMode);
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getNodeAddress() {
		return nodeAddress;
	}

	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	public String getPhysicalAddress() {
		return physicalAddress;
	}

	public void setPhysicalAddress(String physicalAddress) {
		this.physicalAddress = physicalAddress;
	}

	public int getNodeView() {
		return nodeView;
	}

	public void setNodeView(int nodeView) {
		this.nodeView = nodeView;
	}

	public int getClusterSize() {
		return clusterSize;
	}

	public void setClusterSize(int clusterSize) {
		this.clusterSize = clusterSize;
	}

	public String getClusterMembers() {
		return clusterMembers;
	}

	public void setClusterMembers(String clusterMembers) {
		this.clusterMembers = clusterMembers;
	}
}
