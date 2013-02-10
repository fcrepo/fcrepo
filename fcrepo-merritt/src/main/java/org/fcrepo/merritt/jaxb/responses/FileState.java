package org.fcrepo.merritt.jaxb.responses;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class FileState extends AbstractState {
    @XmlElement
    public String versionState;

    @XmlElement
    public double size;

    @XmlElement
    public String messageDigest;

    @XmlElement
    public Date created;

    @XmlElement
    public URI file;
}
