/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * <p>ServletMapping class.</p>
 *
 * @author awoods
 */
public class ServletMapping extends UrlMappable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "servlet-name")
    String servletName;

    public String servletName() {
        return this.servletName;
    }

}
