
package org.fcrepo.jaxb.responses.access;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "objectDatastreams")
public class ObjectDatastreams {

    @XmlElement(name = "datastream")
    public Set<DatastreamElement> datastreams;

    @XmlType(name = "datastream")
    public static class DatastreamElement {

        public DatastreamElement(String dsid, String label, String mimeType) {
            this.dsid = dsid;
            this.label = label;
            this.mimeType = mimeType;
        }

        @XmlAttribute
        public String dsid;

        @XmlAttribute
        public String label;

        @XmlAttribute
        public String mimeType;

        public DatastreamElement() {
        }
    }
}
