package org.fcrepo.merritt.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class VersionState extends AbstractFileContainerEntityState {
    @XmlElement
    public String objectState;

    @XmlElement
    public String localIdentifier;

    @XmlElement
    public String nodeState;

    @XmlElement
    public List<String> fileStates;

    @XmlElement
    public boolean isCurrent;

    @XmlElement
    public URI version;
}
