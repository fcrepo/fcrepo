/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;

/**
 * Builder for constructing an RdfSourceOperation
 *
 * @author bbpennel
 */
public interface RdfSourceOperationBuilder extends RelaxableResourceOperationBuilder {

    @Override
    RdfSourceOperationBuilder userPrincipal(String userPrincipal);

    @Override
    RdfSourceOperation build();

    /**
     * Set the triples for the operation
     *
     * @param triples the resource's triples
     * @return this builder
     */
    RdfSourceOperationBuilder triples(RdfStream triples);

    @Override
    RdfSourceOperationBuilder relaxedProperties(Model model);
}
