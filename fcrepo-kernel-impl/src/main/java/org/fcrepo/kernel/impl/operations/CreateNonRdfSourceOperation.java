/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;

/**
 * Operation for creating a new non-rdf source
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperation extends AbstractNonRdfSourceOperation implements CreateResourceOperation {

    private FedoraId parentId;

    /**
     * Constructor for external content.
     *
     * @param rescId the internal identifier.
     * @param externalContentURI the URI of the external content.
     * @param externalHandling the type of external content handling (REDIRECT, PROXY)
     */
    protected CreateNonRdfSourceOperation(final Transaction transaction, final FedoraId rescId,
                                          final URI externalContentURI, final String externalHandling) {
        super(transaction, rescId, externalContentURI, externalHandling);
    }

    /**
     * Constructor for internal binaries.
     *
     * @param transaction the transaction
     * @param rescId the internal identifier.
     * @param content the stream of the content.
     */
    protected CreateNonRdfSourceOperation(final Transaction transaction, final FedoraId rescId,
                                          final InputStream content) {
        super(transaction, rescId, content);
    }

    @Override
    public String getInteractionModel() {
        return NON_RDF_SOURCE.toString();
    }

    @Override
    public boolean isArchivalGroup() {
        return false;
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

}
