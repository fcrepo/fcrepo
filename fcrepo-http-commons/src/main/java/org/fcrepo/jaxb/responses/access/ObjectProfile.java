
package org.fcrepo.jaxb.responses.access;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "objectProfile")
public class ObjectProfile {

    @XmlAttribute
    public String pid;

    @XmlElement
    public String objLabel;

    @XmlElement
    public String objOwnerId;

    @XmlElementWrapper(name = "objModels")
    @XmlElement(name = "model")
    public Collection<String> objModels;

    @XmlElement
    public Date objCreateDate;

    @XmlElement
    public Date objLastModDate;

    @XmlElement
    public URI objDissIndexViewURL;

    @XmlElement
    public URI objItemIndexViewURL;

    @XmlElement
    public ObjectStates objState;

    @XmlElement
    public Long objSize;

    public static enum ObjectStates {
        A, D, I
    }

}
