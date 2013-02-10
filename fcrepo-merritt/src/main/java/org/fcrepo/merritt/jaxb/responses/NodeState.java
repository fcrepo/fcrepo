package org.fcrepo.merritt.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class NodeState extends AbstractFileContainerEntityState {

    @XmlElement
    public String name;

    @XmlElement
    public String description;

    @XmlElement
    public String nodeVersion;

    @XmlElement
    public double numObjects;

    @XmlElement
    public double numVersions;

    @XmlElement
    public Date lastAddVersion;

    @XmlElement
    public String mediaType;

    @XmlElement
    public String accessMode;

    @XmlElement
    public boolean verifyOnRead;

    @XmlElement
    public boolean verifyOnWrite;

    @XmlElement
    public String nodeScheme;

    @XmlElement
    public URI baseURI;

    @XmlElement
    public URI supportURI;

    public NodeState() {

    }

}
