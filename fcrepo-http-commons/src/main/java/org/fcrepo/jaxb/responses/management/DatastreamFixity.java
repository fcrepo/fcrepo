package org.fcrepo.jaxb.responses.management;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixity")
public class DatastreamFixity {

	@XmlElement
    public long dsSize;
	
	@XmlElement
    public String dsChecksumType;
	
	@XmlElement
    public URI dsChecksum;
	
	@XmlElement
	public boolean validChecksum;
	
	@XmlElement
	public boolean validSize;
}
