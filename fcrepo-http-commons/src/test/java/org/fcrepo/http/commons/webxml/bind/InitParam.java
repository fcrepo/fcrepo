/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.webxml.bind;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>InitParam class.</p>
 *
 * @author awoods
 */
@XmlRootElement(namespace = "http://java.sun.com/xml/ns/javaee",
        name = "init-param")
public class InitParam extends Param {

    public InitParam() {
        super();
    }

    public InitParam(final String name, final String value) {
        super(name, value);
    }

}
