
package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "init-param")
public class InitParam extends Param {

    public InitParam() {
        super();
    }

    public InitParam(String name, String value) {
        super(name, value);
    }

}
