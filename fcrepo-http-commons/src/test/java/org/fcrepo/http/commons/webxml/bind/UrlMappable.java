/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * <p>Abstract UrlMappable class.</p>
 *
 * @author awoods
 */
public abstract class UrlMappable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "url-pattern")
    String urlPattern;

    public String urlPattern() {
        return this.urlPattern;
    }

}
