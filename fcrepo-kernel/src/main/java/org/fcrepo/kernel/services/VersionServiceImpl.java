/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.kernel.services;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fcrepo.kernel.services.TransactionServiceImpl.getCurrentTransactionId;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.propertyContains;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This service exposes management of node versioning.  Instead of invoking
 * the JCR VersionManager methods, this provides a level of indirection that
 * allows for special handling of features built on top of JCR such as user
 * transactions.
 * @author Mike Durbin
 */

@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    private static final Logger LOGGER = getLogger(VersionService.class);

    protected static final String VERSIONABLE = "mix:versionable";

    protected static final String VERSION_POLICY = "fedoraconfig:versioningPolicy";

    protected static final String AUTO_VERSION = "auto-version";

    @Autowired
    private TransactionService txService;

    /**
     * Notifies the version manager that the node at a given path was updated
     * so that if automatic versioning is set for that node, a version
     * checkpoint will be made.  When a node object is available, use of
     * {@link #nodeUpdated(javax.jcr.Node)} will save the overhead of a
     * redundant node lookup.
     * @param session
     * @param absPath the absolute path to the node that was created or
     *                modified
     * @throws RepositoryException
     */
    @Override
    public void nodeUpdated(final Session session, final String absPath) throws RepositoryException {
        nodeUpdated(session.getNode(absPath));
    }

    /**
     * Notifies the version manager that the given node was updated
     * so that if automatic versioning is set for that node, a version
     * checkpoint will be made.
     * @param n the node that was updated
     * @throws RepositoryException
     */
    @Override
    public void nodeUpdated(final Node n) throws RepositoryException {
        if (isVersioningEnabled(n)
                && isImplicitVersioningEnabled(n)) {
            queueOrCommitCheckpoint(n.getSession(), n.getPath());
        } else {
            LOGGER.trace("No implicit version checkpoint set for {}", n.getPath());
        }
    }

    /**
     * Explicitly creates a version for the nodes at each path provided.
     * @param workspace the workspace in which the node resides
     * @param paths absolute paths to the nodes within the workspace
     * @throws RepositoryException
     */
    @Override
    public void createVersion(final Workspace workspace,
            final Collection<String> paths) throws RepositoryException {
        for (final String absPath : paths) {
            checkpoint(workspace, absPath);
        }
    }

    private static boolean isVersioningEnabled(final Node n) throws RepositoryException {
        return n.isNodeType(VERSIONABLE);
    }

    private static boolean isImplicitVersioningEnabled(final Node n) throws RepositoryException {
        if (!n.hasProperty(VERSION_POLICY)) {
            return false;
        }
        final Property p = n.getProperty(VERSION_POLICY);
        return propertyContains(p, AUTO_VERSION);
    }

    private void queueOrCommitCheckpoint(final Session session, final String absPath) throws RepositoryException {
        final String txId = getCurrentTransactionId(session);

        if (txId == null) {
            checkpoint(session.getWorkspace(), absPath);
        } else {
            queueCheckpoint(txId, absPath);
        }
    }

    private static void checkpoint(final Workspace workspace, final String absPath) throws RepositoryException {
        LOGGER.trace("Setting implicit version checkpoint set for {}", absPath);
        workspace.getVersionManager().checkpoint(absPath);
    }

    private void queueCheckpoint(final String txId, final String absPath) throws TransactionMissingException {
        final Transaction tx = txService.getTransaction(txId);
        LOGGER.trace("Queuing implicit version checkpoint set for {}", absPath);
        tx.addPathToVersion(absPath);
    }
    /**
     * Creates a version checkpoint for the given node if versioning is enabled
     * for that node type.  When versioning is enabled this is the equivalent of
     * VersionManager#checkpoint(node.getPath()), except that it is aware of
     * TxSessions and queues these operations accordingly.
     *
     * @param node the node for whom a new version is
     *                to be minted
     * @throws RepositoryException
     */
    @Override
    public void checkpoint(final Node node) throws RepositoryException {

        checkNotNull(node, "Cannot checkpoint null nodes");

        final Session session = node.getSession();
        final String absPath = node.getPath();
        if (node.isNodeType(VERSIONABLE)) {
            LOGGER.trace("Setting checkpoint for {}", absPath);

            final String txId = getCurrentTransactionId(session);
            if (txId != null) {
                final Transaction tx = txService.getTransaction(txId);
                tx.addPathToVersion(absPath);
            } else {
                session.getWorkspace().getVersionManager().checkpoint(absPath);
            }
        } else {
            LOGGER.trace("No checkpoint set for unversionable {}", absPath);
        }
    }

    /**
     * @param txService the txService to set
     */
    @Override
    public void setTxService(final TransactionService txService) {
        this.txService = txService;
    }
}
