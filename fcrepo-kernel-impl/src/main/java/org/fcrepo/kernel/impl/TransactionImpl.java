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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Phaser;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.fcrepo.common.lang.CheckedRunnable;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionState;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.search.api.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Fedora Transaction implementation
 *
 * @author mohideen
 */
public class TransactionImpl implements Transaction {

    private static final Logger log = LoggerFactory.getLogger(TransactionImpl.class);

    private static final RetryPolicy<Object> DB_RETRY = new RetryPolicy<>()
            .handleIf(e -> {
                return e instanceof DeadlockLoserDataAccessException
                        || (e.getCause() != null && e.getCause() instanceof DeadlockLoserDataAccessException);
            })
            .withBackoff(10, 100, ChronoUnit.MILLIS, 1.5)
            .withJitter(0.1)
            .withMaxRetries(10);

    private final String id;

    private final TransactionManagerImpl txManager;

    private TransactionState state;

    private boolean shortLived = true;

    private Instant expiration;

    private boolean expired = false;

    private String baseUri;

    private String userAgent;

    private final Duration sessionTimeout;

    private final Phaser operationPhaser;

    protected TransactionImpl(final String id,
                              final TransactionManagerImpl txManager,
                              final Duration sessionTimeout) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Transaction id should not be empty!");
        }
        this.id = id;
        this.txManager = txManager;
        this.sessionTimeout = sessionTimeout;
        this.expiration = Instant.now().plus(sessionTimeout);
        this.state = TransactionState.OPEN;
        this.operationPhaser = new Phaser();
    }

    @Override
    public synchronized void commit() {
        if (state == TransactionState.COMMITTED) {
            return;
        }
        failIfNotOpen();
        failIfExpired();

        updateState(TransactionState.COMMITTING);

        operationPhaser.register();
        operationPhaser.awaitAdvance(operationPhaser.arriveAndDeregister());

        log.debug("Committing transaction {}", id);

        try {
            if (isShortLived()) {
                doCommitShortLived();
            } else {
                doCommitLongRunning();
            }

            updateState(TransactionState.COMMITTED);
            this.getEventAccumulator().emitEvents(this, baseUri, userAgent);
            releaseLocks();
        } catch (final Exception ex) {
            log.error("Failed to commit transaction: {}", id, ex);

            // Rollback on commit failure
            rollback();
            throw new RepositoryRuntimeException("Failed to commit transaction " + id, ex);
        }
    }

    @Override
    public boolean isCommitted() {
        return state == TransactionState.COMMITTED;
    }

    @Override
    public synchronized void rollback() {
        if (state == TransactionState.ROLLEDBACK || state == TransactionState.ROLLINGBACK) {
            return;
        }

        failIfCommitted();

        log.info("Rolling back transaction {}", id);

        updateState(TransactionState.ROLLINGBACK);

        operationPhaser.register();
        operationPhaser.awaitAdvance(operationPhaser.arriveAndDeregister());

        execQuietly("Failed to rollback storage in transaction " + id, () -> {
            this.getPersistentSession().rollback();
        });
        execQuietly("Failed to rollback index in transaction " + id, () -> {
            this.getContainmentIndex().rollbackTransaction(this);
        });
        execQuietly("Failed to rollback reference index in transaction " + id, () -> {
            this.getReferenceService().rollbackTransaction(this);
        });
        execQuietly("Failed to rollback membership index in transaction " + id, () -> {
            this.getMembershipService().rollbackTransaction(this);
        });
        execQuietly("Failed to rollback search index in transaction " + id, () -> {
            this.getSearchIndex().rollbackTransaction(this);
        });

        execQuietly("Failed to rollback events in transaction " + id, () -> {
            this.getEventAccumulator().clearEvents(this);
        });

        updateState(TransactionState.ROLLEDBACK);

        releaseLocks();
    }

    @Override
    public void doInTx(final Runnable runnable) {
        operationPhaser.register();

        try {
            failIfNotOpen();
            failIfExpired();

            runnable.run();
        } finally {
            operationPhaser.arriveAndDeregister();
        }
    }

    @Override
    public synchronized void fail() {
        if (state != TransactionState.OPEN) {
            log.error("Transaction {} is in state {} and may not be marked as FAILED", id, state);
        } else {
            updateState(TransactionState.FAILED);
        }
    }

    @Override
    public boolean isRolledBack() {
        return state == TransactionState.ROLLEDBACK;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setShortLived(final boolean shortLived) {
        this.shortLived = shortLived;
    }

    @Override
    public boolean isShortLived() {
        return this.shortLived;
    }

    @Override
    public boolean isOpenLongRunning() {
        return !this.isShortLived() && !hasExpired()
                && !(state == TransactionState.COMMITTED
                || state == TransactionState.ROLLEDBACK
                || state == TransactionState.FAILED);
    }

    @Override
    public boolean isOpen() {
        return state == TransactionState.OPEN && !hasExpired();
    }

    @Override
    public void ensureCommitting() {
        if (state != TransactionState.COMMITTING) {
            throw new TransactionRuntimeException(
                    String.format("Transaction %s must be in state COMMITTING, but was %s", id, state));
        }
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void expire() {
        this.expiration = Instant.now();
        this.expired = true;
    }

    @Override
    public boolean hasExpired() {
        if (this.expired) {
            return true;
        }
        this.expired = this.expiration.isBefore(Instant.now());
        return this.expired;
    }

    @Override
    public synchronized Instant updateExpiry(final Duration amountToAdd) {
        failIfExpired();
        failIfCommitted();
        failIfNotOpen();
        this.expiration = this.expiration.plus(amountToAdd);
        return this.expiration;
    }

    @Override
    public Instant getExpires() {
        return this.expiration;
    }

    @Override
    public void commitIfShortLived() {
       if (this.isShortLived()) {
           this.commit();
       }
    }

    @Override
    public void refresh() {
        updateExpiry(sessionTimeout);
    }

    @Override
    public void lockResource(final FedoraId resourceId) {
        getResourceLockManger().acquire(getId(), resourceId);
    }

    @Override
    public void releaseResourceLocksIfShortLived() {
        if (isShortLived()) {
            releaseLocks();
        }
    }

    @Override
    public void setBaseUri(final String baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    private void doCommitShortLived() {
        // short-lived txs do not write to tx tables and do not need to commit db indexes.
        this.getPersistentSession().prepare();
        this.getPersistentSession().commit();
    }

    private void doCommitLongRunning() {
        // MySQL can deadlock when update db records and it must be retried. Unfortunately, the entire transaction
        // must be retried because something marks the transaction for rollback when the exception is thrown
        // regardless if you then retry at the query level.
        Failsafe.with(DB_RETRY).run(() -> {
            // Cannot use transactional annotations because this class is not managed by spring
            getTransactionTemplate().executeWithoutResult(status -> {
                this.getContainmentIndex().commitTransaction(this);
                this.getReferenceService().commitTransaction(this);
                this.getMembershipService().commitTransaction(this);
                this.getSearchIndex().commitTransaction(this);
                this.getPersistentSession().prepare();
                // The storage session must be committed last because mutable head changes cannot be rolled back.
                // The db transaction will remain open until all changes have been written to OCFL. If the changes
                // are large, or are going to S3, this could take some time. In which case, it is possible the
                // db's connection timeout may need to be adjusted so that the connection is not closed while
                // waiting for the OCFL changes to be committed.
                this.getPersistentSession().commit();
            });
        });
    }

    private void updateState(final TransactionState newState) {
        this.state = newState;
    }

    private PersistentStorageSession getPersistentSession() {
        return this.txManager.getPersistentStorageSessionManager().getSession(this);
    }

    private void failIfExpired() {
        if (hasExpired()) {
            throw new TransactionClosedException("Transaction " + id + " expired!");
        }
    }

    private void failIfCommitted() {
        if (state == TransactionState.COMMITTED) {
            throw new TransactionClosedException(
                    String.format("Transaction %s cannot be transitioned because it is already committed!", id));
        }
    }

    private void failIfNotOpen() {
        if (state == TransactionState.FAILED) {
            throw new TransactionRuntimeException(
                    String.format("Transaction %s cannot be committed because it is in a failed state!", id));
        } else if (state != TransactionState.OPEN) {
            throw new TransactionClosedException(
                    String.format("Transaction %s cannot be committed because it is in state %s!", id, state));
        }
    }

    private void releaseLocks() {
        execQuietly("Failed to release resource locks cleanly. You may need to restart Fedora.", () -> {
            getResourceLockManger().releaseAll(getId());
        });
    }

    /**
     * Executes the closure, capturing all exceptions, and logging them as errors.
     *
     * @param failureMessage what to print if the closure fails
     * @param callable closure to execute
     */
    private void execQuietly(final String failureMessage, final CheckedRunnable callable) {
        try {
            callable.run();
        } catch (final Exception e) {
            log.error(failureMessage, e);
        }
    }

    private ContainmentIndex getContainmentIndex() {
        return this.txManager.getContainmentIndex();
    }

    private EventAccumulator getEventAccumulator() {
        return this.txManager.getEventAccumulator();
    }

    private ReferenceService getReferenceService() {
        return this.txManager.getReferenceService();
    }

    private MembershipService getMembershipService() {
        return this.txManager.getMembershipService();
    }

    private SearchIndex getSearchIndex() {
        return this.txManager.getSearchIndex();
    }

    private TransactionTemplate getTransactionTemplate() {
        return this.txManager.getTransactionTemplate();
    }

    private ResourceLockManager getResourceLockManger() {
        return this.txManager.getResourceLockManager();
    }

    @Override
    public String toString() {
        return id;
    }
}
