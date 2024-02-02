/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>FilterMapping class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "filter-mapping")
public class FilterMapping extends UrlMappable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-name")
    String filterName;

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "servlet-name")
    String servletName;

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "dispatcher")
    String dispatcher;

    public String filterName() {
        return this.filterName;
    }

    public String servletName() {
        return this.servletName;
    }

    public String dispatcher() {
        return this.dispatcher;
    }

}
