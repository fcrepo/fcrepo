package org.fcrepo.jaxb.responses.access;

import static org.fcrepo.services.functions.GetClusterConfiguration.CACHE_MODE;
import static org.fcrepo.services.functions.GetClusterConfiguration.CLUSTER_MEMBERS;
import static org.fcrepo.services.functions.GetClusterConfiguration.CLUSTER_NAME;
import static org.fcrepo.services.functions.GetClusterConfiguration.CLUSTER_SIZE;
import static org.fcrepo.services.functions.GetClusterConfiguration.NODE_ADDRESS;
import static org.fcrepo.services.functions.GetClusterConfiguration.NODE_VIEW;
import static org.fcrepo.services.functions.GetClusterConfiguration.PHYSICAL_ADDRESS;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "clusterConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class DescribeCluster {

	@XmlElement
	public String cacheMode;

	@XmlElement
	public String clusterName;

	@XmlElement
	public String nodeAddress;

	@XmlElement
	public String physicalAddress;

	@XmlElement
	public String nodeView;

	@XmlElement
	public String clusterSize;

	@XmlElement
	public String clusterMembers;

	public DescribeCluster() {

	}

	public DescribeCluster(Map<String, String> in) {
		this.setCacheMode(in.get(CACHE_MODE));
		this.setClusterMembers(in.get(CLUSTER_MEMBERS));
		this.setClusterName(in.get(CLUSTER_NAME));
		this.setClusterSize(in.get(CLUSTER_SIZE));
		this.setNodeAddress(in.get(NODE_ADDRESS));
		this.setNodeView(in.get(NODE_VIEW));
		this.setPhysicalAddress(in.get(PHYSICAL_ADDRESS));
	}

	//Getters and setters mainly used for velocity template access
	public String getCacheMode() {
		return cacheMode;
	}

	public void setCacheMode(String cacheMode) {
		this.cacheMode = cacheMode;
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

	public String getNodeView() {
		return nodeView;
	}

	public void setNodeView(String nodeView) {
		this.nodeView = nodeView;
	}

	public String getClusterSize() {
		return clusterSize;
	}

	public void setClusterSize(String clusterSize) {
		this.clusterSize = clusterSize;
	}

	public String getClusterMembers() {
		return clusterMembers;
	}

	public void setClusterMembers(String clusterMembers) {
		this.clusterMembers = clusterMembers;
	}
}