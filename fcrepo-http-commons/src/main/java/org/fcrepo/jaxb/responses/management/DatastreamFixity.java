package org.fcrepo.jaxb.responses.management;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixity")
public class DatastreamFixity {

	@XmlElement(name = "DatastreamFixityStatus")
    public List<FixityStatus> statuses;
	
}
