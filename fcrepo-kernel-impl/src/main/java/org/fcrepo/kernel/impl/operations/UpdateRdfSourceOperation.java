/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationType;


/**
 * Operation for updating an RDF source
 *
 * @author bbpennel
 */
public class UpdateRdfSourceOperation extends AbstractRdfSourceOperation {

    protected UpdateRdfSourceOperation(final Transaction transaction, final FedoraId rescId, final RdfStream triples) {
        super(transaction, rescId, triples);
    }

    @Override
    public ResourceOperationType getType() {
        return UPDATE;
    }
}
