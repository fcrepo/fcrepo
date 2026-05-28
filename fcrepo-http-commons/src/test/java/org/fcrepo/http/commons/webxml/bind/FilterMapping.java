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
@XmlRootElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
        name = "filter-mapping")
public class FilterMapping extends UrlMappable {

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
            name = "filter-name")
    String filterName;

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
            name = "servlet-name")
    String servletName;

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
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
