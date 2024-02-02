/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import static java.util.Collections.emptyList;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;

/**
 * <p>Filter class.</p>
 *
 * @author awoods
 */
public class Filter extends Displayable {

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "init-param")})
    List<InitParam> initParams;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-name")})
    String filterName;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "filter-class")})
    String filterClass;

    public String filterName() {
        return this.filterName;
    }

    public String filterClass() {
        return this.filterClass;
    }

    public List<InitParam> initParams() {
        if (initParams != null) {
            return initParams;
        }
        return emptyList();
    }

}
