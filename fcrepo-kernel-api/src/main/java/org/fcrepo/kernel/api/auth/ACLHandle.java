/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.auth;

import java.util.List;

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Class to hold the authorizations from an ACL and the resource that has the ACL.
 *
 * @author whikloj
 */
public interface ACLHandle {

    /**
     * Get the resource that contains the ACL.
     *
     * @return the fedora resource
     */
    FedoraResource getResource();

    /**
     * Get the list of authorizations from the ACL.
     *
     * @return the authorizations
     */
    List<WebACAuthorization> getAuthorizations();
}
