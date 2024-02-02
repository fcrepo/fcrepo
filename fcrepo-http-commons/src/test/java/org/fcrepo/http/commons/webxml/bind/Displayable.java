/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * <p>Abstract Displayable class.</p>
 *
 * @author awoods
 */
public abstract class Displayable extends Describable {

    @XmlElement(namespace = "http://java.sun.com/xml/ns/javaee",
            name = "display-name")
    String displayName;

    public String displayName() {
        return this.displayName;
    }
}
