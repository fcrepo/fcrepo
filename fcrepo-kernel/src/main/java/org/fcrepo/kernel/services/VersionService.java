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

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This service exposes management of node versioning.  Instead of invoking
 * the JCR VersionManager methods, this provides a level of indirection that
 * allows for special handling of features built on top of JCR such as user
 * transactions.
 * @author Mike Durbin
 */

@Component
public class VersionService extends RepositoryService {

    private static final Logger logger = getLogger(VersionService.class);

    protected static final String VERSIONABLE = "mix:versionable";

    protected static final String VERSION_POLICY = "fedora:versioning-policy";

    protected static final String AUTO_VERSION = "auto-version";

    @Autowired
    TransactionService txService;

    /**
     * Notifies the version manager that the node at a given path was updated
     * so that if automatic versioning is set for that node, a version
     * checkpoint will be made.
     * @param session
     * @param absPath the absolute path to the node that was created or
     *                modified
     * @throws RepositoryException
     */
    public void nodeUpdated(final Session session, String absPath) throws RepositoryException {
        Node n = session.getNode(absPath);
        if (isVersioningEnabled(n)
                && isImplicitVersioningEnabled(n)) {
            queueOrCommitCheckpoint(session, absPath);
        } else {
            logger.trace("No implicit version checkpoint set for {}", absPath);
        }
    }

    /**
     * Explicitly creates a version for the nodes at each path provided.
     * @param workspace the workspace in which the node resides
     * @param paths absolute paths to the nodes within the workspace
     * @throws RepositoryException
     */
    public void createVersion(final Workspace workspace,
            Collection<String> paths) throws RepositoryException {
        for (String absPath : paths) {
            checkpoint(workspace, absPath);
        }
    }

    private boolean isVersioningEnabled(Node n) throws RepositoryException {
        return n.isNodeType(VERSIONABLE);
    }

    private boolean isImplicitVersioningEnabled(Node n) throws RepositoryException {
        if (!n.hasProperty(VERSION_POLICY)) {
            return false;
        } else {
            for (Value val : n.getProperty(VERSION_POLICY).getValues()) {
                if (AUTO_VERSION.equals(val.getString())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void queueOrCommitCheckpoint(Session session, String absPath) throws RepositoryException {
        String txId = TransactionService.getCurrentTransactionId(session);
        if (txId == null) {
            checkpoint(session.getWorkspace(), absPath);
        } else {
            queueCheckpoint(txId, absPath);
        }
    }

    private void checkpoint(Workspace workspace, String absPath) throws RepositoryException {
        logger.trace("Setting implicit version checkpoint set for {}", absPath);
        workspace.getVersionManager().checkpoint(absPath);
    }

    private void queueCheckpoint(String txId, String absPath) throws TransactionMissingException {
        Transaction tx = txService.getTransaction(txId);
        logger.trace("Queuing implicit version checkpoint set for {}", absPath);
        tx.addPathToVersion(absPath);
    }
}
