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
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>Servlet class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "listener")
public class Servlet extends Displayable {

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "init-param")})
    List<InitParam> initParams;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "servlet-name")})
    String servletName;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "servlet-class")})
    String servletClass;

    @XmlElements(value = {@XmlElement(
            namespace = "http://java.sun.com/xml/ns/javaee",
            name = "load-on-startup")})
    String loadOnStartUp;

    public String servletName() {
        return this.servletName;
    }

    public String servletClass() {
        return this.servletClass;
    }

    public List<InitParam> initParams() {
        if (initParams != null) {
            return initParams;
        }
        return emptyList();
    }
}
