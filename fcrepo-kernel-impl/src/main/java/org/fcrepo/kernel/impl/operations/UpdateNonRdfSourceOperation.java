/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationType;

/**
 * Operation for updating a non-rdf source
 *
 * @author bbpennel
 */
public class UpdateNonRdfSourceOperation extends AbstractNonRdfSourceOperation {

    /**
     * Constructor for internal binaries.
     *
     * @param rescId the internal identifier.
     * @param content the stream of the content.
     */
    protected UpdateNonRdfSourceOperation(final Transaction transaction, final FedoraId rescId,
                                          final InputStream content) {
        super(transaction, rescId, content);
    }

    /**
     * Constructor for external content.
     *
     * @param transaction the transaction
     * @param rescId the internal identifier.
     * @param externalContentURI the URI of the external content.
     * @param externalHandling the type of external content handling (REDIRECT, PROXY)
     */
    protected UpdateNonRdfSourceOperation(final Transaction transaction, final FedoraId rescId,
                                          final URI externalContentURI, final String externalHandling) {
        super(transaction, rescId, externalContentURI, externalHandling);
    }

    @Override
    public ResourceOperationType getType() {
        return UPDATE;
    }
}
