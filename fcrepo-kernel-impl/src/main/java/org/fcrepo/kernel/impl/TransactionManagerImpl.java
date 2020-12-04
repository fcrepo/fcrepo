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
package org.fcrepo.kernel.impl;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionNotFoundException;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.UUID.randomUUID;

/**
 * The Fedora Transaction Manager implementation
 *
 * @author mohideen
 */
@Component
public class TransactionManagerImpl implements TransactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManagerImpl.class);

    private final Map<String, Transaction> transactions;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Inject
    private PersistentStorageSessionManager pSessionManager;

    @Inject
    private EventAccumulator eventAccumulator;

    @Autowired
    @Qualifier("referenceService")
    private ReferenceService referenceService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private ResourceLockManager resourceLockManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void postConstruct() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    TransactionManagerImpl() {
        transactions = new ConcurrentHashMap<>();
    }

    /**
     * Periodically scan for closed transactions for cleanup
     */
    @Scheduled(fixedDelayString = "#{fedoraPropsConfig.sessionTimeout}")
    public void cleanupClosedTransactions() {
        LOGGER.trace("Cleaning up expired transactions");

        final var txIt = transactions.entrySet().iterator();
        while (txIt.hasNext()) {
            final var txEntry = txIt.next();
            final var tx = txEntry.getValue();

            // Cleanup if transaction is closed and past its expiration time
            if (tx.isCommitted() || tx.isRolledBack()) {
                if (tx.hasExpired()) {
                    txIt.remove();
                }
            } else if (tx.hasExpired()) {
                LOGGER.debug("Rolling back expired transaction {}", tx.getId());
                try {
                    // If the tx has expired but is not already closed, then rollback
                    // but don't immediately remove it from the list of transactions
                    // so that the rolled back status can be checked
                    tx.rollback();
                } catch (final RuntimeException e) {
                    LOGGER.error("Failed to rollback expired transaction {}", tx.getId(), e);
                }
            }

            if (tx.hasExpired()) {
                // By this point the session as already been committed or rolledback by the transaction
                pSessionManager.removeSession(tx.getId());
            }
        }
    }

    @Override
    public synchronized Transaction create() {
        String txId = randomUUID().toString();
        while (transactions.containsKey(txId)) {
            txId = randomUUID().toString();
        }
        final Transaction tx = new TransactionImpl(txId, this);
        transactions.put(txId, tx);
        return tx;
    }

    @Override
    public Transaction get(final String transactionId) {
        if (transactions.containsKey(transactionId)) {
            final Transaction transaction = transactions.get(transactionId);
            if (transaction.hasExpired()) {
                transaction.rollback();
                throw new TransactionClosedException("Transaction with transactionId: " + transactionId +
                    " expired at " + transaction.getExpires() + "!");
            }
            if (transaction.isCommitted()) {
                throw new TransactionClosedException("Transaction with transactionId: " + transactionId +
                        " has already been committed.");
            }
            if (transaction.isRolledBack()) {
                throw new TransactionClosedException("Transaction with transactionId: " + transactionId +
                        " has already been rolled back.");
            }
            return transaction;
        } else {
            throw new TransactionNotFoundException("No Transaction found with transactionId: " + transactionId);
        }
    }

    protected PersistentStorageSessionManager getPersistentStorageSessionManager() {
        return pSessionManager;
    }

    protected ContainmentIndex getContainmentIndex() {
        return containmentIndex;
    }

    protected EventAccumulator getEventAccumulator() {
        return eventAccumulator;
    }

    protected ReferenceService getReferenceService() {
        return referenceService;
    }

    protected MembershipService getMembershipService() {
        return membershipService;
    }

    protected TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    protected ResourceLockManager getResourceLockManager() {
        return resourceLockManager;
    }

}
