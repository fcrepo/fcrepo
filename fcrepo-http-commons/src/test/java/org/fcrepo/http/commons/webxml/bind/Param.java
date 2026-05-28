/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * <p>Param class.</p>
 *
 * @author awoods
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Param extends Describable {

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
            name = "param-name")
    String name;

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee",
            name = "param-value")
    String value;

    public Param() {
    }

    public Param(final String name, final String value) {
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
    public boolean equals(final Object object) {
        if (this.getClass().equals(object.getClass())) {
            final Param that = (Param) object;
            final boolean name = Objects.equals(this.name, that.name);
            final boolean value = Objects.equals(this.value, that.value);
            return name && value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + 2 * value.hashCode();

    }
}
