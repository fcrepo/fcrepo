
package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlElement;

public abstract class UrlMappable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "url-pattern")
    String urlPattern;

    public String urlPattern() {
        return this.urlPattern;
    }

}
