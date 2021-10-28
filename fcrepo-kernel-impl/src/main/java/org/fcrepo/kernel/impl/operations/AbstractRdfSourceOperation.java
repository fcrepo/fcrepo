/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;

/**
 * Abstract operation for interacting with an rdf source
 *
 * @author bbpennel
 */
public abstract class AbstractRdfSourceOperation extends AbstractRelaxableResourceOperation
                                                 implements RdfSourceOperation {

    protected RdfStream triples;

    protected AbstractRdfSourceOperation(final Transaction transaction, final FedoraId rescId,
                                         final RdfStream triples) {
        super(transaction, rescId);
        this.triples = triples;
    }

    @Override
    public RdfStream getTriples() {
        return triples;
    }
}
