/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.models.FedoraResource;

import java.util.stream.Stream;

/**
 * Interface for a service that converts managed properties from a {@link org.fcrepo.kernel.api.models.FedoraResource}
 * into a triple stream
 *
 * @author dbernstein
 * @since 2020-01-07
 */
public interface ManagedPropertiesService {

    /**
     * Retrieve the managed properties as triples
     *
     * @param resource The fedora resource
     * @return A stream of managed properties for the resource.
     */
    Stream<Triple> get(FedoraResource resource);

}