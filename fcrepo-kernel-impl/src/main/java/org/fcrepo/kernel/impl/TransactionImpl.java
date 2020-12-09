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

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.fcrepo.common.lang.CheckedRunnable;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * The Fedora Transaction implementation
 *
 * @author mohideen
 */
public class TransactionImpl implements Transaction {

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

    private static final Logger log = LoggerFactory.getLogger(TransactionImpl.class);

    private static final RetryPolicy<Object> DB_RETRY = new RetryPolicy<>()
            .handleIf(e -> {
                return e instanceof DeadlockLoserDataAccessException
                        || (e.getCause() != null && e.getCause() instanceof DeadlockLoserDataAccessException);
            })
            .withBackoff(50, 1000, ChronoUnit.MILLIS, 1.5)
            .withJitter(0.1)
            .withMaxRetries(5);

    private static final Duration DEFAULT_TIMEOUT = ofMinutes(3);

    private final String id;

    private final TransactionManagerImpl txManager;

    private boolean shortLived = true;

    private Instant expiration;

    private boolean expired = false;

    private boolean rolledback = false;

    private boolean committed = false;

    private String baseUri;

    private String userAgent;

    protected TransactionImpl(final String id, final TransactionManagerImpl txManager) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Transaction id should not be empty!");
        }
        this.id = id;
        this.txManager = txManager;
        this.expiration = Instant.now().plus(timeout());
    }

    @Override
    public synchronized void commit() {
        failIfExpired();
        failIfRolledback();
        if (this.committed) {
            return;
        }
        try {
            log.debug("Committing transaction {}", id);
            // MySQL can deadlock when update db records and it must be retried. Unfortunately, the entire transaction
            // must be retried because something marks the transaction for rollback when the exception is thrown
            // regardless if you then retry at the query level.
            Failsafe.with(DB_RETRY).run(() -> {
                // Cannot use transactional annotations because this class is not managed by spring
                getTransactionTemplate().executeWithoutResult(status -> {
                    this.getContainmentIndex().commitTransaction(id);
                    this.getReferenceService().commitTransaction(id);
                    this.getMembershipService().commitTransaction(id);
                    this.getPersistentSession().prepare();
                    // The storage session must be committed last because mutable head changes cannot be rolled back.
                    // The db transaction will remain open until all changes have been written to OCFL. If the changes
                    // are large, or are going to S3, this could take some time. In which case, it is possible the
                    // db's connection timeout may need to be adjusted so that the connection is not closed while
                    // waiting for the OCFL changes to be committed.
                    this.getPersistentSession().commit();
                });
            });
            this.getEventAccumulator().emitEvents(id, baseUri, userAgent);
            this.committed = true;
            releaseLocks();
        } catch (final Exception ex) {
            log.error("Failed to commit transaction: {}", id, ex);

            // Rollback on commit failure
            rollback();
            throw new RepositoryRuntimeException("Failed to commit transaction " + id, ex);
        }
    }

    @Override
    public synchronized boolean isCommitted() {
        return committed;
    }

    @Override
    public synchronized void rollback() {
        failIfCommitted();
        if (this.rolledback) {
            return;
        }
        log.info("Rolling back transaction {}", id);
        this.rolledback = true;

        execQuietly("Failed to rollback storage in transaction " + id, () -> {
            this.getPersistentSession().rollback();
        });
        execQuietly("Failed to rollback index in transaction " + id, () -> {
            this.getContainmentIndex().rollbackTransaction(id);
        });
        execQuietly("Failed to rollback reference index in transaction " + id, () -> {
            this.getReferenceService().rollbackTransaction(id);
        });
        execQuietly("Failed to rollback membership index in transaction " + id, () -> {
            this.getMembershipService().rollbackTransaction(id);
        });
        execQuietly("Failed to rollback events in transaction " + id, () -> {
            this.getEventAccumulator().clearEvents(id);
        });

        releaseLocks();
    }

    @Override
    public synchronized boolean isRolledBack() {
        return rolledback;
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
    public synchronized void expire() {
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
        failIfRolledback();
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
        updateExpiry(timeout());
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

    private Duration timeout() {
        // Get the configured timeout
        final String timeoutProperty = System.getProperty(TIMEOUT_SYSTEM_PROPERTY);
        if (timeoutProperty != null) {
            return ofMillis(Long.parseLong(timeoutProperty));
        } else {
            // Otherwise, use the default timeout
            return DEFAULT_TIMEOUT;
        }
    }

    private PersistentStorageSession getPersistentSession() {
        return this.txManager.getPersistentStorageSessionManager().getSession(this.id);
    }

    private void failIfExpired() {
        if (hasExpired()) {
            throw new TransactionClosedException("Transaction with transactionId: " + id + " expired!");
        }
    }

    private void failIfCommitted() {
        if (this.committed) {
            throw new TransactionClosedException("Transaction with transactionId: " + id + " is already committed!");
        }
    }

    private void failIfRolledback() {
        if (this.rolledback) {
            throw new TransactionClosedException("Transaction with transactionId: " + id + " is already rolledback!");
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

    private TransactionTemplate getTransactionTemplate() {
        return this.txManager.getTransactionTemplate();
    }

    private ResourceLockManager getResourceLockManger() {
        return this.txManager.getResourceLockManager();
    }

}
