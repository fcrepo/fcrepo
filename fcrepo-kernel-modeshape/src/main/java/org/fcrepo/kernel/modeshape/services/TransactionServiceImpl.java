/**
 * Copyright 2015 DuraSpace, Inc.
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
/**
 *
 */

package org.fcrepo.kernel.modeshape.services;

import static com.google.common.collect.Maps.filterValues;
import static java.lang.System.currentTimeMillis;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.collect.ImmutableSet;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TxSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionMissingException;
import org.fcrepo.kernel.api.services.TransactionService;
import org.fcrepo.kernel.modeshape.TransactionImpl;

import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This is part of the strawman implementation for Fedora transactions This
 * service implements a simple {@link Transaction} service which is able to
 * create/commit/rollback {@link Transaction} objects A {@link Scheduled}
 * annotation is used for removing timed out Transactions
 *
 * @author frank asseg
 * @author ajs6f
 */
@Component
public class TransactionServiceImpl extends AbstractService implements TransactionService {

    private static final Logger LOGGER = getLogger(TransactionServiceImpl.class);

    /**
     * A key for looking up the transaction id in a session key-value pair
     */
    static final String FCREPO4_TX_ID = "fcrepo4.tx.id";

    /**
     * TODO since transactions have to be available on all nodes, they have to
     * be either persisted or written to a distributed map or sth, not just this
     * plain hashmap that follows
     */
    private static Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    public static final long REAP_INTERVAL = 1000;

    /**
     * Check if a session is possibly within a transaction
     * @param session the session
     * @return whether the session is possibly within a transaction
     */
    public static boolean isInTransaction(final Session session) {
        try {
            return ImmutableSet.copyOf(session.getNamespacePrefixes()).contains(FCREPO4_TX_ID);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Every REAP_INTERVAL milliseconds, check for expired transactions. If the
     * tx is expired, roll it back and remove it from the registry.
     */
    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.services.TransactionService#removeAndRollbackExpired()
     */
    @Override
    @Scheduled(fixedRate = REAP_INTERVAL)
    public void removeAndRollbackExpired() {
        synchronized (transactions) {
            filterValues(transactions, tx -> tx.getExpires().getTime() <= currentTimeMillis())
                    .forEach((key, tx) -> {
                        try {
                            tx.rollback();
                        } catch (final RepositoryRuntimeException e) {
                            LOGGER.error("Got exception rolling back expired transaction {}: {}", tx, e.getMessage());
                        }
                        transactions.remove(key);
                    });
        }
    }

    /**
     * Create a new Transaction and add it to the currently open ones
     *
     * @param sess The session to use for this Transaction
     * @return the {@link Transaction}
     */
    @Override
    public Transaction beginTransaction(final Session sess, final String userName) {
        final Transaction tx = new TransactionImpl(sess, userName);
        final String txId = tx.getId();
        transactions.put(txId, tx);
        try {
            sess.setNamespacePrefix(FCREPO4_TX_ID, txId);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return tx;
    }

    @Override
    public Transaction getTransaction(final String txId, final String userName) {
        final Transaction tx = transactions.computeIfAbsent(txId, s -> {
            throw new TransactionMissingException("Transaction with id: " + s + " is not available");
        });
        if (!tx.isAssociatedWithUser(userName)) {
            throw new TransactionMissingException("Transaction with id " +
                        txId + " is not available for user " + userName);
        }
        return tx;
    }

    /**
     * Get the current Transaction for a session
     *
     * @param session the session
     * @return the given session's current Transaction
     * @throws TransactionMissingException if transaction missing exception occurred
     */
    @Override
    public Transaction getTransaction(final Session session) {
        final String txId = getCurrentTransactionId(session);

        if (txId == null) {
            throw new TransactionMissingException(
                    "Transaction is not available");
        }
        return transactions.computeIfAbsent(txId, s -> {
            throw new TransactionMissingException("Transaction with id: " + s + " is not available");
        });
    }

    /**
     * Get the current Transaction ID for a session
     *
     * @param session the session
     * @return the current Transaction ID for the given session
     */
    public static String getCurrentTransactionId(final Session session) {
        try {
            if (session instanceof TxSession) {
                return ((TxSession) session).getTxId();
            }
            return session.getNamespaceURI(FCREPO4_TX_ID);
        } catch (final RepositoryException e) {
            LOGGER.trace("Unable to retrieve current transaction ID from session: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a Transaction exists
     *
     * @param txid the Id of the {@link Transaction}
     * @return the {@link Transaction}
     */
    @Override
    public boolean exists(final String txid) {
        return transactions.containsKey(txid);
    }

    /**
     * Commit a {@link Transaction} with the given id
     *
     * @param txid the id of the {@link Transaction}
     */
    @Override
    public Transaction commit(final String txid) {
        final Transaction tx = transactions.remove(txid);
        if (tx == null) {
            throw new TransactionMissingException("Transaction with id " + txid +
                    " is not available");
        }
        tx.commit();
        return tx;
    }

    /**
     * Roll a {@link Transaction} back
     *
     * @param txid the id of the {@link Transaction}
     * @return the {@link Transaction} object
     */
    @Override
    public Transaction rollback(final String txid) {
        final Transaction tx = transactions.remove(txid);
        if (tx == null) {
            throw new TransactionMissingException("Transaction with id " + txid +
                    " is not available");
        }
        tx.rollback();
        return tx;
    }

}
