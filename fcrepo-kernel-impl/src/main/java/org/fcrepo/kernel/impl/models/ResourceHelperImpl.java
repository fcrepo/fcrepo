/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.models.ResourceHelper;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Utility class for helper methods.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class ResourceHelperImpl implements ResourceHelper {

    private static final Logger LOGGER = getLogger(ResourceHeadersImpl.class);

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Override
    public boolean isGhostNode(final Transaction transaction, final FedoraId resourceId) {
        if (!doesResourceExist(transaction, resourceId, true)) {
            return containmentIndex.hasResourcesStartingWith(transaction, resourceId);
        }
        return false;
    }

    @Override
    public boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                     final boolean includeDeleted) {
        if (fedoraId.isRepositoryRoot()) {
            // Root always exists.
            return true;
        }
        if (!(fedoraId.isMemento() || fedoraId.isAcl())) {
            // containment index doesn't handle versions and only tells us if the resource (not acl) is there,
            // so don't bother checking for them.
            return containmentIndex.resourceExists(transaction, fedoraId, includeDeleted);
        } else {

            final PersistentStorageSession psSession = getSession(transaction);

            try {
                // Resource ID for metadata or ACL contains their individual endopoints (ie. fcr:metadata, fcr:acl)
                final ResourceHeaders headers = psSession.getHeaders(fedoraId, fedoraId.getMementoInstant());
                return !headers.isDeleted();
            } catch (final PersistentItemNotFoundException e) {
                // Object doesn't exist.
                return false;
            } catch (final PersistentStorageException e) {
                // Other error, pass along.
                throw new RepositoryRuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * Get a session for this interaction.
     *
     * @param transaction The supplied transaction.
     * @return a storage session.
     */
    private PersistentStorageSession getSession(final Transaction transaction) {
        final PersistentStorageSession session;
        if (transaction.isReadOnly() || !transaction.isOpen()) {
            session = persistentStorageSessionManager.getReadOnlySession();
        } else {
            session = persistentStorageSessionManager.getSession(transaction);
        }
        return session;
    }
}
