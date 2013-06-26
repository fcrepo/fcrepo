
package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
