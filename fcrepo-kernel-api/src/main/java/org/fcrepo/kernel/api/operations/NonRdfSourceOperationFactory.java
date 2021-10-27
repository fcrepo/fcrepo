/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.io.InputStream;
import java.net.URI;

/**
 * Factory for constructing operations on non-rdf sources
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for a external binary update operation
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param handling the type of handling to be used for the external binary content
     * @param contentUri the URI of the external binary content
     * @return a new builder
     */
    NonRdfSourceOperationBuilder updateExternalBinaryBuilder(Transaction transaction, FedoraId rescId, String handling,
                                                             URI contentUri);

    /**
     * Get a builder for an internal binary update operation
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param contentStream inputstream for the content of this binary
     * @return a new builder
     */
    NonRdfSourceOperationBuilder updateInternalBinaryBuilder(Transaction transaction, FedoraId rescId,
                                                             InputStream contentStream);

    /**
     * Get a builder for a external binary create operation
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param handling the type of handling to be used for the external binary content
     * @param contentUri the URI of the external binary content
     * @return a new builder
     */
    CreateNonRdfSourceOperationBuilder createExternalBinaryBuilder(Transaction transaction, FedoraId rescId,
                                                                   String handling,
                                                                   URI contentUri);

    /**
     * Get a builder for an internal binary create operation
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param contentStream inputstream for the content of this binary
     * @return a new builder
     */
    CreateNonRdfSourceOperationBuilder createInternalBinaryBuilder(Transaction transaction, FedoraId rescId,
                                                                   InputStream contentStream);

    /**
     * Get a builder for an operation to update headers of a Non-RDF resource
     *
     * @param transaction the transaction
     * @param resourceId id of the resource targeted by the operation
     * @param serverManagedPropsMode server managed props mode
     * @return new builder
     */
    UpdateNonRdfSourceHeadersOperationBuilder updateHeadersBuilder(Transaction transaction,
                                                          FedoraId resourceId,
                                                          ServerManagedPropsMode serverManagedPropsMode);
}
