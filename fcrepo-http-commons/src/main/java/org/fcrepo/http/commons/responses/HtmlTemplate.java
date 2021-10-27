/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation that hints to the HtmlProvider a template that should be used
 * to render a response.
 *
 * @author awoods
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface HtmlTemplate {

    /**
     * @return The name of the HTML template to render for this method
     */
    String value();
}
