/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionNotFoundException;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.search.api.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.UUID.randomUUID;

/**
 * The Fedora Transaction Manager implementation
 *
 * @author mohideen
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class TransactionManagerImpl implements TransactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManagerImpl.class);

    private final Map<String, Transaction> transactions;

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
    @Qualifier("searchIndex")
    private SearchIndex searchIndex;

    @Inject
    private ResourceLockManager resourceLockManager;

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

    @Inject
    private DbTransactionExecutor dbTransactionExecutor;

    @Inject
    private UserTypesCache userTypesCache;

    TransactionManagerImpl() {
        transactions = new ConcurrentHashMap<>();
    }

    /**
     * Periodically scan for closed transactions for cleanup
     */
    @Scheduled(fixedDelayString = "#{fedoraPropsConfig.sessionTimeout}")
    public void cleanupClosedTransactions() {
        LOGGER.debug("Cleaning up expired transactions");

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
        final Transaction tx = new TransactionImpl(txId, this, fedoraPropsConfig.getSessionTimeout());
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

    @PreDestroy
    public void cleanupAllTransactions() {
        LOGGER.debug("Shutting down transaction manager, attempt to rollback any incomplete transactions");
        final var txIt = transactions.entrySet().iterator();
        while (txIt.hasNext()) {
            final var txEntry = txIt.next();
            final var tx = txEntry.getValue();

            if ((tx.isOpen() || tx.hasExpired()) && !tx.isRolledBack() ) {
                LOGGER.debug("Rolling back transaction as part of shutdown {}", tx.getId());
                try {
                    tx.rollback();
                    pSessionManager.removeSession(tx.getId());
                } catch (TransactionClosedException ignore) {
                    // ignore. Already committed
                    LOGGER.info("Failed to rollback transaction {} as already committed", tx.getId(), ignore);
                } catch (final RuntimeException e) {
                    LOGGER.error("Failed to rollback transaction {}", tx.getId(), e);
                }
            }
        }
        LOGGER.debug("Finished rollback of all incomplete transactions as part of shut down");
    }

    @PostConstruct
    public void preCleanTransactions() {
        LOGGER.debug("TransactionManagerImpl initialized, cleaning up leftover transaction entries");

        // Clean up any leftover transaction database entries immediately after startup
        synchronized (transactions) {
            containmentIndex.clearAllTransactions();
            membershipService.clearAllTransactions();
            referenceService.clearAllTransactions();
            searchIndex.clearAllTransactions();
            // Also clear any leftover ocfl sessions and staged files
            pSessionManager.clearAllSessions();
        }
    }

    protected PersistentStorageSessionManager getPersistentStorageSessionManager() {
        return pSessionManager;
    }

    protected ContainmentIndex getContainmentIndex() {
        return containmentIndex;
    }

    protected SearchIndex getSearchIndex() {
        return searchIndex;
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

    protected ResourceLockManager getResourceLockManager() {
        return resourceLockManager;
    }

    protected UserTypesCache getUserTypesCache() {
        return userTypesCache;
    }

    public DbTransactionExecutor getDbTransactionExecutor() {
        return dbTransactionExecutor;
    }
}
