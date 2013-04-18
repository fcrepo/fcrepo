
package org.fcrepo.jaxb.responses.management;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "datastreamHistory")
public class DatastreamHistory {

    @XmlAttribute
    public String pid;

    @XmlAttribute
    public String dsID;

    @XmlElement
    List<DatastreamProfile> datastreamProfiles;

    public DatastreamHistory() {
    }

    public DatastreamHistory(final List<DatastreamProfile> datastreamProfiles) {
        this.datastreamProfiles = datastreamProfiles;
    }
}
