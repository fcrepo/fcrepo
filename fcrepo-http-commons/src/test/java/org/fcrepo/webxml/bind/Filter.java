package org.fcrepo.webxml.bind;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public class Filter extends Displayable {
	
	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="init-param")})
	List<InitParam> initParams;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="filter-name")})
	String filterName;
	
	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="filter-class")})
	String filterClass;

	public String filterName() {
		return this.filterName;
	}
	
	public String filterClass() {
		return this.filterClass;
	}
	
	@SuppressWarnings("unchecked")
	public List<InitParam> initParams() {
		return (initParams != null) ? initParams : Collections.EMPTY_LIST;
	}

}
