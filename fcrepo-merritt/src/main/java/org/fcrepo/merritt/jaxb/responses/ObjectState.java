package org.fcrepo.merritt.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ObjectState extends AbstractFileContainerEntityState {
    @XmlElement
    public String localContext;

    @XmlElement
    public String localIdentifier;

    @XmlElement
    public String nodeState;

    @XmlElement
    public List<String> versionStates;

    @XmlElement
    public String currentVersionState;

    @XmlElement
    public double numVersions;

    @XmlElement
    public Date lastAddVersion;

    @XmlElement
    public URI object;

    @XmlElement
    public String objectScheme;


    public ObjectState() {

    }
}
