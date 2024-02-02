/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>Listener class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "listener")
public class Listener extends Displayable {

    public Listener() {
    }

    public Listener(final String displayName, final String className) {
        this.displayName = displayName;
        this.className = className;
    }

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "listener-class")
    String className;

    public String className() {
        return this.className;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof Listener) {
            final Listener that = (Listener) object;
            final boolean className =
                (this.className == null) ? that.className == null
                    : this.className.equals(that.className);
            final boolean displayName =
                (this.displayName == null) ? that.displayName == null
                    : this.displayName.equals(that.displayName);
            return className && displayName;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + 2 * displayName.hashCode();
    }

}
