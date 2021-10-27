/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.apache.jena.rdf.model.Model;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;

/**
 * Builder for operations to update rdf sources
 *
 * @author bbpennel
 * @since 11/2019
 */
public class UpdateRdfSourceOperationBuilder extends AbstractRdfSourceOperationBuilder {

    /**
     * Constructor.
     *
     * @param transaction the transaction/
     * @param resourceId the internal identifier.
     * @param serverManagedPropsMode server managed properties mode
     */
    public UpdateRdfSourceOperationBuilder(final Transaction transaction, final FedoraId resourceId,
                                           final ServerManagedPropsMode serverManagedPropsMode) {
        super(transaction, resourceId, null, serverManagedPropsMode);
    }

    @Override
    public RdfSourceOperation build() {
        final var operation = new UpdateRdfSourceOperation(this.transaction, this.rescId, this.tripleStream);
        operation.setUserPrincipal(userPrincipal);
        operation.setCreatedBy(createdBy);
        operation.setCreatedDate(createdDate);
        operation.setLastModifiedBy(lastModifiedBy);
        operation.setLastModifiedDate(lastModifiedDate);
        return operation;
    }

    @Override
    public UpdateRdfSourceOperationBuilder relaxedProperties(final Model model) {
        super.relaxedProperties(model);
        return this;
    }
}
