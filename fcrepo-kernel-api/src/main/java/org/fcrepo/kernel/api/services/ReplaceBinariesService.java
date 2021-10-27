/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;

/**
 * Interface for service to replace existing binaries
 *
 * @author mohideen
 */
public interface ReplaceBinariesService {

    /**
     * Replace an existing binary.
     *
     * @param tx The transaction for the request.
     * @param userPrincipal the user performing the service
     * @param fedoraId The internal identifier of the parent.
     * @param filename The filename of the binary.
     * @param contentType The content-type header or null if none.
     * @param digests The binary digest or null if none.
     * @param size The binary size.
     * @param contentBody The request body or null if none.
     * @param externalContent The external content handler or null if none.
     */
    void perform(Transaction tx,
                 String userPrincipal,
                 FedoraId fedoraId,
                 String filename,
                 String contentType,
                 Collection<URI> digests,
                 InputStream contentBody,
                 long size,
                 ExternalContent externalContent);
}
