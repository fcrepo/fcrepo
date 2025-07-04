/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.ws.rs.HttpMethod;

/**
 * PATCH HTTP method
 *
 * @author awoods
 */
@Target({METHOD})
@Retention(RUNTIME)
@HttpMethod("PATCH")
public @interface PATCH {
}
