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
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

    @Autowired
    TransactionService txService;

    /**
     * Creates a version checkpoint for the given path if versioning is enabled
     * for that node type.  When versioning is enabled this is the equivalent of
     * VersionManager#checkpoint(path), except that it is aware of
     * TxSessions and queues these operations accordingly.
     *
     * @param session
     * @param absPath the absolute path to the node for whom a new version is
     *                to be minted
     * @throws RepositoryException
     */
    public void checkpoint(final Session session, String absPath) throws RepositoryException {
        if (session.getNode(absPath).isNodeType(VERSIONABLE)) {
            logger.trace("Setting checkpoint for {}", absPath);

            String txId = TransactionService.getCurrentTransactionId(session);
            if (txId != null) {
                Transaction tx = txService.getTransaction(txId);
                tx.addPathToVersion(absPath);
            } else {
                session.getWorkspace().getVersionManager().checkpoint(absPath);
            }
        } else {
            logger.trace("No checkpoint set for unversionable {}", absPath);
        }
    }
}
