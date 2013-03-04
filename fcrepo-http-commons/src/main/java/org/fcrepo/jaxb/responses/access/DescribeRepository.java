
package org.fcrepo.jaxb.responses.access;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "fedoraRepository")
public class DescribeRepository {

    public static final String FEDORA_VERSION = "4.0-modeshape-candidate";

    @XmlElement
    protected String repositoryVersion = FEDORA_VERSION;

    @XmlElement
    public Long numberOfObjects;

    @XmlElement
    public Long repositorySize;

}
