
package org.fcrepo.webxml.bind;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

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

    @SuppressWarnings("unchecked")
    public List<InitParam> initParams() {
        return (initParams != null) ? initParams : Collections.EMPTY_LIST;
    }
}
