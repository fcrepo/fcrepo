/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.net.URI;
import java.util.Collection;

/**
 * Builder for operations to create non-rdf sources
 *
 * @author bbpennel
 */
public interface CreateNonRdfSourceOperationBuilder extends NonRdfSourceOperationBuilder {

    @Override
    CreateNonRdfSourceOperationBuilder mimeType(String mimetype);

    @Override
    CreateNonRdfSourceOperationBuilder filename(String filename);

    @Override
    CreateNonRdfSourceOperationBuilder contentDigests(Collection<URI> digests);

    @Override
    CreateNonRdfSourceOperationBuilder contentSize(long size);

    /**
     * Set the parent identifier of the resource
     *
     * @param parentId parent internal identifier
     * @return the builder
     */
    CreateNonRdfSourceOperationBuilder parentId(FedoraId parentId);

    @Override
    CreateNonRdfSourceOperationBuilder userPrincipal(String userPrincipal);

}
