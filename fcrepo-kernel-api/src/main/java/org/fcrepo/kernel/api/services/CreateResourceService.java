/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Interface for a service to create a new resource via a POST request.
 * @author whikloj
 * @since 2019-11-05
 */
public interface CreateResourceService {

    /**
     * Create a new NonRdfSource resource.
     *
     * @param tx The transaction for the request.
     * @param userPrincipal the principal of the user performing the service
     * @param fedoraId The internal identifier of the resource.
     * @param contentType The content-type header or null if none.
     * @param filename The original filename of the binary
     * @param contentSize The size of the content stream
     * @param linkHeaders The original LINK headers or null if none.
     * @param digest The binary digest or null if none.
     * @param requestBody The request body or null if none.
     * @param externalContent The external content handler or null if none.
     */
    void perform(Transaction tx, String userPrincipal, FedoraId fedoraId,
                 String contentType, String filename, long contentSize, List<String> linkHeaders,
                 Collection<URI> digest, InputStream requestBody, ExternalContent externalContent);

    /**
     * Create a new RdfSource resource.
     *
     * @param tx The transaction for the request.
     * @param userPrincipal the principal of the user performing the service
     * @param fedoraId The internal identifier of the resource
     * @param linkHeaders The original LINK headers or null if none.
     * @param model The request body RDF as a Model
     */
    void perform(Transaction tx, String userPrincipal, FedoraId fedoraId,
            List<String> linkHeaders, Model model);

}
