package org.fcrepo.merritt.jaxb.responses;


import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

abstract public class AbstractFileContainerEntityState extends AbstractState {
    @XmlElement
    public double numFiles;

    @XmlElement
    public double totalSize;

    @XmlElement
    public double numActualFiles;

    @XmlElement
    public double totalActualSize;

    @XmlElement
    public Date created;

    @XmlElement
    public Date lastModified;

}
