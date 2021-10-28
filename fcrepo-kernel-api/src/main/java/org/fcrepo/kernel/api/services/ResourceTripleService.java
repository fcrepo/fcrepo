/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;

/**
 * Service to call other services to return a desired set of triples.
 * @author whikloj
 * @since 6.0.0
 */
public interface ResourceTripleService {

    /**
     * Return the triples for the resource based on the Prefer: header preferences
     * @param tx The transaction or null if none.
     * @param resource the resource to get triples for.
     * @param preferences the preferences asked for.
     * @param limit limit on the number of children to display.
     * @return a stream of triples.
     */
    Stream<Triple> getResourceTriples(final Transaction tx, final FedoraResource resource,
                                      final LdpTriplePreferences preferences, final int limit);
}
