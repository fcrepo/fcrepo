/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;

/**
 * Operation to create an RDF source.
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationImpl extends AbstractRdfSourceOperation implements CreateRdfSourceOperation {

    private FedoraId parentId;

    /**
     * The interaction model
     */
    private String interactionModel;

    private boolean archivalGroup = false;

    /**
     * Constructor for creation operation
     *
     * @param transaction the transaction
     * @param rescId the internal identifier.
     * @param interactionModel interaction model for the resource
     * @param triples triples stream for the resource
     */
    protected CreateRdfSourceOperationImpl(final Transaction transaction, final FedoraId rescId,
                                           final String interactionModel, final RdfStream triples) {
        super(transaction, rescId, triples);
        this.interactionModel = interactionModel;
    }

    @Override
    public String getInteractionModel() {
        return interactionModel;
    }

    @Override
    public boolean isArchivalGroup() {
        return this.archivalGroup;
    }

    @Override
    public FedoraId getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(final FedoraId parentId) {
        this.parentId = parentId;
    }

    /**
     *
     * @param flag flag indicating whether resource is an Archival Group
     */
    public void setArchivalGroup(final boolean flag) {
        this.archivalGroup = flag;
    }


}
