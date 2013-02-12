package org.fcrepo.merritt.jaxb.responses;


import javax.xml.bind.annotation.XmlElement;

abstract public class AbstractState {
    @XmlElement
    public String identifier;
}
