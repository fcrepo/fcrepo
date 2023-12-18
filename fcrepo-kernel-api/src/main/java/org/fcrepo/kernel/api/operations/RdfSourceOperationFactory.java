/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;


import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Factory for operations on rdf sources
 *
 * @author bbpennel
 */
public interface RdfSourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for an operation to create an RDF source
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param interactionModel interaction model for the resource being created
     * @param serverManagedPropsMode server managed props mode
     * @return new builder
     */
    CreateRdfSourceOperationBuilder createBuilder(Transaction transaction,
                                                  FedoraId rescId,
                                                  String interactionModel,
                                                  ServerManagedPropsMode serverManagedPropsMode);

    /**
     * Get a builder for an operation to create an RDF source
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param interactionModel interaction model for the resource being created
     * @param serverManagedPropsMode server managed props mode
     * @param isOverwrite if a tombstone is being overwritten
     * @return new builder
     */
    CreateRdfSourceOperationBuilder createBuilder(Transaction transaction,
                                                  FedoraId rescId,
                                                  String interactionModel,
                                                  ServerManagedPropsMode serverManagedPropsMode,
                                                  boolean isOverwrite);

    /**
     * Get a builder for an operation to update an RDF source
     *
     * @param transaction the transaction
     * @param rescId id of the resource targeted by the operation
     * @param serverManagedPropsMode server managed props mode
     * @return new builder
     */
    RdfSourceOperationBuilder updateBuilder(Transaction transaction, FedoraId rescId,
                                            final ServerManagedPropsMode serverManagedPropsMode);
}
