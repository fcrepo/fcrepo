/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import java.util.List;

import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.auth.WebACAuthorization;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * A simple class connecting an URI pointing to an ACL to a {@link FedoraResource} that points to that ACL.
 *
 * @author ajs6f
 */
public class ACLHandleImpl implements ACLHandle {

    private final FedoraResource resource;

    private final List<WebACAuthorization> authorizations;
    /**
     * Default constructor.
     *
     * @param resource the requested FedoraResource
     * @param authorizations any authorizations associated with the uri
     */
    public ACLHandleImpl(final FedoraResource resource, final List<WebACAuthorization> authorizations) {
        this.resource = resource;
        this.authorizations = authorizations;
    }

    @Override
    public FedoraResource getResource() {
        return resource;
    }

    @Override
    public List<WebACAuthorization> getAuthorizations() {
        return authorizations;
    }
}
