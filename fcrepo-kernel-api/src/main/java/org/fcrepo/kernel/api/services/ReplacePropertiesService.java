/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface ReplacePropertiesService {

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param tx the Transaction
     * @param userPrincipal the user performing the service
     * @param fedoraId the internal Id of the fedora resource to update
     * @param inputModel the model built from the body of the request
     * @throws MalformedRdfException if malformed rdf exception occurred
     */
    void perform(Transaction tx,
                 String userPrincipal,
                 FedoraId fedoraId,
                 Model inputModel) throws MalformedRdfException;
}
