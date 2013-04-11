
package org.fcrepo.jaxb.responses.access;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fedoraRepository")
@XmlAccessorType(XmlAccessType.FIELD)
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

    public URI getRepositoryBaseURL() {
        return repositoryBaseURL;
    }

    public void setRepositoryBaseURL(URI repositoryBaseURL) {
        this.repositoryBaseURL = repositoryBaseURL;
    }

    public String getRepositoryVersion() {
        return repositoryVersion;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }

    public Long getNumberOfObjects() {
        return numberOfObjects;
    }

    public void setNumberOfObjects(Long numberOfObjects) {
        this.numberOfObjects = numberOfObjects;
    }

    public Long getRepositorySize() {
        return repositorySize;
    }

    public void setRepositorySize(Long repositorySize) {
        this.repositorySize = repositorySize;
    }

    public URI getSampleOAIURL() {
        return sampleOAIURL;
    }

    public void setSampleOAIURL(URI sampleOAIURL) {
        this.sampleOAIURL = sampleOAIURL;
    }

}
