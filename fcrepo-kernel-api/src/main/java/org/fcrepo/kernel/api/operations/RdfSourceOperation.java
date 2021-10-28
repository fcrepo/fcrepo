/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.RdfStream;

/**
 * Operation for interacting with an rdf source
 *
 * @author bbpennel
 */
public interface RdfSourceOperation extends RelaxableResourceOperation {

    /**
     * Get the incoming user space triples for the resource
     *
     * @return triples
     */
    RdfStream getTriples();

}
