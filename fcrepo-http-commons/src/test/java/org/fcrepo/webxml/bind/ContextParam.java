package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace="http://java.sun.com/xml/ns/javaee", name="context-param")
public class ContextParam extends Param {

	public ContextParam() {
		super();
	}
	
	public ContextParam(String name, String value) {
		super(name, value);
	}

}
