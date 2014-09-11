/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.fcrepo.kernel.services.TransactionService;
import org.fcrepo.kernel.services.VersionService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.util.Collection;
import java.util.HashSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fcrepo.kernel.impl.services.TransactionServiceImpl.getCurrentTransactionId;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.propertyContains;
import static org.slf4j.LoggerFactory.getLogger;

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
        if (isImplicitVersioningEnabled(n)) {
            if (!isVersioningEnabled(n)) {
                enableVersioning(n);
            }
            queueOrCommitCheckpoint(n.getSession(), n.getPath());
        } else {
            LOGGER.trace("No implicit version checkpoint set for {}", n.getPath());
        }
    }

    /**
     * Explicitly creates a version for the nodes at each path provided.
     * If the node doesn't have the versionable mixing, that mixin is
     * added.
     * @param workspace the workspace in which the node resides
     * @param paths absolute paths to the nodes within the workspace
     * @throws RepositoryException
     */
    @Override
    public Collection<String> createVersion(final Workspace workspace,
            final Collection<String> paths) throws RepositoryException {
        final Collection<String> versions = new HashSet<>();
        for (final String absPath : paths) {
            final Node node = workspace.getSession().getNode(absPath);
            if (!isVersioningEnabled(node)) {
                enableVersioning(node);
            }
            versions.add( checkpoint(workspace, absPath) );
        }
        return versions;
    }

    @Override
    public void revertToVersion(final Workspace workspace, final String absPath,
                                final String label) throws RepositoryException {
        final Version v = getVersionForLabel(workspace, absPath, label);
        if (v == null) {
            throw new PathNotFoundException("Unknown version \"" + label + "\"!");
        }
        final VersionManager versionManager = workspace.getVersionManager();
        versionManager.checkin(absPath);
        versionManager.restore(v, true);
        versionManager.checkout(absPath);

        nodeUpdated(workspace.getSession(), absPath);
    }

    @Override
    public void removeVersion(final Workspace workspace, final String absPath,
                              final String label) throws RepositoryException {
        final Version v = getVersionForLabel(workspace, absPath, label);

        if (v == null) {
            throw new PathNotFoundException("Unknown version \"" + label + "\"!");
        } else if (workspace.getVersionManager().getBaseVersion(absPath).equals(v) ) {
            throw new VersionException("Cannot remove current version");
        } else {
            // remove labels
            final VersionHistory history = v.getContainingHistory();
            final String[] versionLabels = history.getVersionLabels(v);
            for ( final String versionLabel : versionLabels ) {
                LOGGER.debug("Removing label: {}", versionLabel);
                history.removeVersionLabel( versionLabel );
            }
            history.removeVersion( v.getName() );
        }
    }


    private static Version getVersionForLabel(final Workspace workspace, final String absPath,
                                       final String label) throws RepositoryException {
        // first see if there's a version label
        final VersionHistory history = workspace.getVersionManager().getVersionHistory(absPath);

        if (history.hasVersionLabel(label)) {
            return history.getVersionByLabel(label);
        }
        // there was no version with the given JCR Version Label, check to see if
        // there's a version whose UUID is equal to the label
        final VersionIterator versionIt = history.getAllVersions();
        if (versionIt == null) {
            return null;
        }
        while (versionIt.hasNext()) {
            final Version v = versionIt.nextVersion();
            if (v.getFrozenNode().getIdentifier().equals(label)) {
                return v;
            }
        }
        return null;
    }

    private static boolean isVersioningEnabled(final Node n) throws RepositoryException {
        return n.isNodeType(VERSIONABLE);
    }

    private static void enableVersioning(final Node node) throws RepositoryException {
        node.addMixin(VERSIONABLE);
        node.getSession().save();
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
            queueCheckpoint(session, absPath);
        }
    }

    private static String checkpoint(final Workspace workspace, final String absPath) throws RepositoryException {
        LOGGER.trace("Setting implicit version checkpoint set for {}", absPath);
        final Version v = workspace.getVersionManager().checkpoint(absPath);
        return ( v == null ) ? null : v.getFrozenNode().getIdentifier();
    }

    private void queueCheckpoint(final Session session, final String absPath) throws TransactionMissingException {
        final Transaction tx = txService.getTransaction(session);
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
                final Transaction tx = txService.getTransaction(session);
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
