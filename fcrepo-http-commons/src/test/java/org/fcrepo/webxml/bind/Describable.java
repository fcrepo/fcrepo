package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlElement;


public abstract class Describable {
	
	@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="description")
    String description;

    public String getDescription() {
    	return description;
    }
    
}
