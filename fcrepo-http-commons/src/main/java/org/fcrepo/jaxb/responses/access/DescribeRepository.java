
package org.fcrepo.jaxb.responses.access;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fedoraRepository")
public class DescribeRepository {

    public static final String FEDORA_VERSION = "4.0-modeshape-candidate";

    @XmlElement
    public URI repositoryBaseURL;

    @XmlElement
    protected String repositoryVersion = FEDORA_VERSION;

    @XmlElement
    public Long numberOfObjects;

    @XmlElement
    public Long repositorySize;

    @XmlElement(name = "sampleOAI-URL")
    public URI sampleOAIURL;

}
