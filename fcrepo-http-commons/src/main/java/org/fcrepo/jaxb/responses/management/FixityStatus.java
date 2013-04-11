
package org.fcrepo.jaxb.responses.management;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixityStatus")
public class FixityStatus {

    @XmlElement
    public String storeIdentifier;

    @XmlElement
    public long computedSize;

    @XmlElement
    public URI computedChecksum;

    @XmlElement
    public long dsSize;

    @XmlElement
    public String dsChecksumType;

    @XmlElement
    public URI dsChecksum;

    @XmlElement
    public boolean validChecksum;

    @XmlElement
    public boolean validSize;
}
