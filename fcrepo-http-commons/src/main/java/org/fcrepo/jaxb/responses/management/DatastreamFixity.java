
package org.fcrepo.jaxb.responses.management;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fcrepo.utils.FixityResult;

@XmlRootElement(name = "DatastreamFixity")
public class DatastreamFixity {

    @XmlAttribute(name = "dsId")
    public String dsId;

    @XmlAttribute(name = "objectId")
    public String objectId;

    @XmlAttribute(name = "timestamp")
    public Date timestamp;

    @XmlElement(name = "DatastreamFixityStatus")
    public List<FixityResult> statuses;

}
