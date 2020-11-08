/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.models;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionUtils;
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
            return containmentIndex.hasResourcesStartingWith(TransactionUtils.openTxId(transaction), resourceId);
        }
        return false;
    }

    @Override
    public boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                     final boolean includeDeleted) {
        final String transactionId = TransactionUtils.openTxId(transaction);
        if (fedoraId.isRepositoryRoot()) {
            // Root always exists.
            return true;
        }
        if (!(fedoraId.isMemento() || fedoraId.isAcl())) {
            // containment index doesn't handle versions and only tells us if the resource (not acl) is there,
            // so don't bother checking for them.
            return containmentIndex.resourceExists(transactionId, fedoraId, includeDeleted);
        } else {

            final PersistentStorageSession psSession = getSession(transactionId);

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
     * @param transactionId The supplied transaction id.
     * @return a storage session.
     */
    private PersistentStorageSession getSession(final String transactionId) {
        final PersistentStorageSession session;
        if (transactionId == null) {
            session = persistentStorageSessionManager.getReadOnlySession();
        } else {
            session = persistentStorageSessionManager.getSession(transactionId);
        }
        return session;
    }
}
