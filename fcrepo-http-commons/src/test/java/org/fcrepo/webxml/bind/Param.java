
package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Param extends Describable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "param-name")
    String name;

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "param-value")
    String value;

    public Param() {
    }

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this.getClass().equals(object.getClass())) {
            Param that = (Param) object;
            boolean name =
                    (this.name == null) ? that.name == null : this.name
                            .equals(that.name);
            boolean value =
                    (this.value == null) ? that.value == null : this.value
                            .equals(that.value);
            return name && value;
        }
        return false;
    }
}
