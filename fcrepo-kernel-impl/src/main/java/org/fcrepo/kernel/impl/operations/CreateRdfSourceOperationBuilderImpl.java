/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.apache.jena.rdf.model.Model;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;

/**
 * Builder for operations to create rdf sources
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationBuilderImpl extends AbstractRdfSourceOperationBuilder implements
        CreateRdfSourceOperationBuilder {

    private FedoraId parentId;

    private boolean archivalGroup = false;
    private boolean isOverwrite = false;

    /**
     * Constructor.
     *
     * @param transaction the transaction
     * @param resourceId the internal identifier.
     * @param interactionModel interaction model
     * @param serverManagedPropsMode server managed props mode
     */
    public CreateRdfSourceOperationBuilderImpl(final Transaction transaction, final FedoraId resourceId,
                                               final String interactionModel,
                                               final ServerManagedPropsMode serverManagedPropsMode) {
        super(transaction, resourceId, interactionModel, serverManagedPropsMode);
    }

    @Override
    public CreateRdfSourceOperation build() {
        final var operation =
            isOverwrite ? new OverwriteRdfTombstoneOperation(transaction, rescId, interactionModel, tripleStream)
                        : new CreateRdfSourceOperationImpl(transaction, rescId, interactionModel, tripleStream);
        operation.setParentId(parentId);
        operation.setUserPrincipal(userPrincipal);
        operation.setCreatedBy(createdBy);
        operation.setCreatedDate(createdDate);
        operation.setLastModifiedBy(lastModifiedBy);
        operation.setLastModifiedDate(lastModifiedDate);
        operation.setArchivalGroup(archivalGroup);
        return operation;
    }

    @Override
    public CreateRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        super.userPrincipal(userPrincipal);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder triples(final RdfStream triples) {
        super.triples(triples);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder parentId(final FedoraId parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder relaxedProperties(final Model model) {
        super.relaxedProperties(model);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder archivalGroup(final boolean flag) {
        this.archivalGroup = flag;
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder isOverwrite(final boolean isOverwrite) {
        this.isOverwrite = isOverwrite;
        return this;
    }

}
