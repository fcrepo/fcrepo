package org.fcrepo.modeshape.jaxb.responses;

import java.net.URI;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "objectProfile", namespace = "http://www.fedora.info/definitions/1/0/access/")
public class ObjectProfile {

	@XmlAttribute
	public String pid;
	
	@XmlElement
	public String objLabel;

	@XmlElement
	public String objOwnerId;
	
	@XmlElement
	public Long objSize;

	@XmlElementWrapper(name = "objModels")
	@XmlElement(name = "model")
	public Collection<String> objModels;

	@XmlElement
	public String objCreateDate;

	@XmlElement
	public String objLastModDate;

	@XmlElement
	public URI objDissIndexViewURL;

	@XmlElement
	public URI objItemIndexViewURL;

	@XmlElement
	public ObjectStates objState;

	public static enum ObjectStates {
		A, D, I
	}

}
