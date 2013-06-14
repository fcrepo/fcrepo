/**
 *
 */

package org.fcrepo.services;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Transaction;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is part of the Strawman implementation for Fedora transactions
 * This service  implements a simple {@link Transaction} service which is able to
 * create/commit/rollback {@link Transaction} objects
 *
 * A {@link Scheduled} annotation is used for removing timed out Transactions
 * @author frank asseg
 *
 */
@Component
public class TransactionService {

    private static final Logger LOGGER = getLogger(TransactionService.class);

    /*
     * TODO: since transactions have to be available on all nodes, they have to
     * be either persisted or written to a distributed map or sth, not just
     * this plain hashmap that follows
     */
    private static Map<String, Transaction> TRANSACTIONS =
            new ConcurrentHashMap<String, Transaction>();

    public static final long REAP_INTERVAL = 1000;

    @Scheduled(fixedRate = REAP_INTERVAL)
    public void removeAndRollbackExpired() {
        synchronized (TRANSACTIONS) {
            Iterator<Entry<String, Transaction>> txs =
                    TRANSACTIONS.entrySet().iterator();
            while (txs.hasNext()) {
                Transaction tx = txs.next().getValue();
                if (tx.getExpires().getTime() <= System.currentTimeMillis()) {
                    try {
                        tx.rollback();
                    } catch (RepositoryException e) {
                        LOGGER.warn("Got exception rolling back expired transaction {}: {}", tx, e);
                    }
                    txs.remove();
                }
            }
        }
    }

    /**
     * Create a new Transaction and add it to the currently open ones
     * @param sess The session to use for this Transaction
     * @return the {@link Transaction}
     */
    public Transaction beginTransaction(final Session sess) {
        final Transaction tx = new Transaction(sess);
        TRANSACTIONS.put(tx.getId(), tx);
        return tx;
    }

    /**
     * Retrieve an open {@link Transaction}
     * @param txid the Id of the {@link Transaction}
     * @return the {@link Transaction}
     * @throws PathNotFoundException if the {@link Transaction} with the given id has not be found
     */
    public Transaction getTransaction(final String txid)
            throws PathNotFoundException {
        final Transaction tx = TRANSACTIONS.get(txid);
        if (tx == null) {
            throw new PathNotFoundException("Transaction is not available");
        }
        return tx;
    }

    /**
     * Check if a Transaction exists
     * @param txid the Id of the {@link Transaction}
     * @return the {@link Transaction}
     * @throws PathNotFoundException if the {@link Transaction} with the given id has not be found
     */
    public boolean exists(final String txid) {
        return TRANSACTIONS.containsKey(txid);
    }

    /**
     * Commit a {@link Transaction} with the given id
     * @param txid the id of the {@link Transaction}
     * @throws RepositoryException
     */
    public Transaction commit(final String txid) throws RepositoryException {
        final Transaction tx = TRANSACTIONS.remove(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        tx.commit();
        return tx;
    }

    /**
     * Roll a {@link Transaction} back
     * @param txid the id of the {@link Transaction}
     * @return the {@link Transaction} object
     * @throws RepositoryException if the {@link Transaction} could not be found
     */
    public Transaction rollback(final String txid) throws RepositoryException {
        final Transaction tx = TRANSACTIONS.remove(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        tx.rollback();
        return tx;
    }

}
