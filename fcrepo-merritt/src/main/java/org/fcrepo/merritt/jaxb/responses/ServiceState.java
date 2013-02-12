package org.fcrepo.merritt.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ServiceState extends AbstractFileContainerEntityState {

    @XmlElement
    public String name;

    @XmlElement
    public String description;

    @XmlElement
    public String serviceVersion;

    @XmlElement
    public List<String> nodeStates;

    @XmlElement
    public double numObjects;

    @XmlElement
    public double numVersions;

    @XmlElement
    public Date lastAddVersion;

    @XmlElement
    public String serviceScheme;

    @XmlElement
    public URI baseURI;

    @XmlElement
    public URI supportURI;

    public ServiceState() {

    }

}
