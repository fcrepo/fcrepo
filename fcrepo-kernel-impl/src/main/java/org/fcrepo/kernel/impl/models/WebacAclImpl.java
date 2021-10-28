/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

/**
 * Webac Acl class
 *
 * @author whikloj
 */
public class WebacAclImpl extends ContainerImpl implements WebacAcl {

    /**
     * Constructor
     * @param fedoraID the internal identifier
     * @param transaction the current transactionId
     * @param pSessionManager a session manager
     * @param resourceFactory a resource factory instance.
     * @param userTypesCache the user types cache
     */
    public WebacAclImpl(final FedoraId fedoraID,
                        final Transaction transaction,
                        final PersistentStorageSessionManager pSessionManager,
                        final ResourceFactory resourceFactory,
                        final UserTypesCache userTypesCache) {
        super(fedoraID, transaction, pSessionManager, resourceFactory, userTypesCache);
    }

    @Override
    public FedoraResource getContainer() {
        final var originalId = FedoraId.create(getFedoraId().getBaseId());
        try {

            return resourceFactory.getResource(transaction, originalId);
        } catch (final PathNotFoundException exc) {
            throw new PathNotFoundRuntimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public boolean isOriginalResource() {
        return false;
    }

    @Override
    public boolean isAcl() {
        return true;
    }
}
