/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.OverwriteTombstoneOperation;

/**
 * @author mikejritter
 */
public class OverwriteRdfTombstoneOperation extends CreateRdfSourceOperationImpl
    implements OverwriteTombstoneOperation {

    protected OverwriteRdfTombstoneOperation(final Transaction transaction, final FedoraId rescId,
                                             final String interactionModel, final RdfStream triples) {
        super(transaction, rescId, interactionModel, triples);
    }

}
