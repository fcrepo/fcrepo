package org.fcrepo.modeshape.jaxb.responses;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "datastreamProfile", namespace = "http://www.fedora.info/definitions/1/0/management/")
public class DatastreamProfile {

	@XmlAttribute
	public String pid;

	@XmlAttribute
	public String dsID;

	@XmlAttribute
	public String dsLabel;
	
	@XmlAttribute
	public String dsVersionID;

	@XmlAttribute
	public String dsCreateDate;

	@XmlAttribute
	public DatastreamStates dsState;

	@XmlAttribute
	public String dsMIME;

	@XmlAttribute
	public URI dsFormatURI;
	
	@XmlAttribute
	public DatastreamControlGroup dsControlGroup;
	
	@XmlAttribute
	public long dsSize;
	
	@XmlAttribute
	public String dsVersionable;
	
	@XmlAttribute
	public String dsInfoType;
	
	@XmlAttribute
	public String dsLocation;
	
	@XmlAttribute
	public String dsLocationType;
	
	@XmlAttribute
	public String dsChecksumType;
	
	@XmlAttribute
	public String dsChecksum;
	
	public static enum DatastreamControlGroup {
		M, E, R
	}
	
	public static enum DatastreamStates {
		A, D, I
	}

}
