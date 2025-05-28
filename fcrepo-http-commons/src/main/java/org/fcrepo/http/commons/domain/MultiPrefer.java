/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import java.util.Set;

import jakarta.ws.rs.HeaderParam;

/**
 * Aggregates information from multiple Prefer HTTP headers.
 *
 * @author ajs6f
 * @since 23 October 2014
 */
public class MultiPrefer extends SinglePrefer {

    /**
     * @param header the header
     */
    public MultiPrefer(final String header) {
        super(header);
    }

    /**
     * @param prefers the prefers
     */
    public MultiPrefer(final @HeaderParam("Prefer") Set<SinglePrefer> prefers) {
        super("");
        prefers.forEach(p -> preferTags().addAll(p.preferTags()));
    }
}
