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

/**
 * Provides containment triples.
 *
 * @author whikloj
 * @since 6.0.0
 */
public interface ContainmentTriplesService {

    /**
     * Retrieve the containment triples.
     *
     * @param tx The transaction or null if none.
     * @param resource The fedora container resource in which children resources are contained.
     * @return A stream of containment triples for the resource.
     */
    Stream<Triple> get(Transaction tx, FedoraResource resource);

    /**
     * Retrieve containment triples which have the provided resource as the object
     *
     * @param tx The transaction or null if none.
     * @param objectResource The object resource to retrieve containment triples for.
     * @return A stream of containment triples for the resource.
     */
    Stream<Triple> getContainedBy(Transaction tx, FedoraResource objectResource);
}
