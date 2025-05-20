/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>Listener class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
        name = "listener")
public class Listener extends Displayable {

    public Listener() {
    }

    public Listener(final String displayName, final String className) {
        this.displayName = displayName;
        this.className = className;
    }

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
            name = "listener-class")
    String className;

    public String className() {
        return this.className;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof Listener) {
            final Listener that = (Listener) object;
            final boolean className = Objects.equals(this.className, that.className);
            final boolean displayName = Objects.equals(this.displayName, that.displayName);
            return className && displayName;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + 2 * displayName.hashCode();
    }

}
