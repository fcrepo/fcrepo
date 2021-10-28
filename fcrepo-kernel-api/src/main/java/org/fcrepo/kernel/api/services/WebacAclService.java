/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.services;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.WebacAcl;

/**
 * Service for creating and retrieving {@link WebacAcl}
 *
 * @author peichman
 * @author whikloj
 * @since 6.0.0
 */
public interface WebacAclService {

    /**
     * Retrieve an existing WebACL by transaction and path
     *
     * @param fedoraId the fedoraID to the resource the ACL is part of
     * @param transaction the transaction
     * @return retrieved ACL
     */
    WebacAcl find(final Transaction transaction, final FedoraId fedoraId);

    /**
     * Retrieve or create a new WebACL by transaction and path
     *
     * @param transaction the transaction
     * @param fedoraId the fedoraID to the resource the ACL is part of
     * @param userPrincipal the user creating the ACL.
     * @param model the contents of the ACL RDF.
     */
    void create(final Transaction transaction, final FedoraId fedoraId, final String userPrincipal,
                    final Model model);
}
