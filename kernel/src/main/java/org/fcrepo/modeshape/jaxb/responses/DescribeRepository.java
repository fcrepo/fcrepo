package org.fcrepo.modeshape.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fedoraRepository", namespace = "http://www.fedora.info/definitions/1/0/access/")
public class DescribeRepository {

	public static final String FEDORA_VERSION = "4.0-modeshape-candidate";

	@XmlElement
	protected String repositoryVersion = FEDORA_VERSION;

	@XmlElement
	public Long numberOfObjects;

	@XmlElement
	public Long repositorySize;

}
