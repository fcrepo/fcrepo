/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * @author bbpennel
 *
 */
public interface CreateRdfSourceOperationBuilder extends RdfSourceOperationBuilder {

    @Override
    CreateRdfSourceOperationBuilder userPrincipal(String userPrincipal);

    @Override
    CreateRdfSourceOperationBuilder triples(RdfStream triples);

    @Override
    CreateRdfSourceOperationBuilder relaxedProperties(Model model);

    @Override
    CreateRdfSourceOperation build();

    /**
     * Set the parent identifier of the resource
     *
     * @param parentId parent internal identifier
     * @return the builder
     */
    CreateRdfSourceOperationBuilder parentId(FedoraId parentId);

    /**
     * Indicates that this resource should be created as an Archival Group
     * @param flag if true, create as Archival Group
     * @return this builder
     */
    CreateRdfSourceOperationBuilder archivalGroup(boolean flag);

    /**
     * @param isOverwrite flag indicating if this resource is a tombstone and is being overwritten
     * @return this builder
     */
    CreateRdfSourceOperationBuilder isOverwrite(boolean isOverwrite);

}
