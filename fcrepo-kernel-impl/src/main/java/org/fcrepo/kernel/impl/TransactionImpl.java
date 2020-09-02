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
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;

/**
 * The Fedora Transaction implementation
 *
 * @author mohideen
 */
public class TransactionImpl implements Transaction {

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

    private static final Logger log = LoggerFactory.getLogger(TransactionImpl.class);

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
            this.getPersistentSession().commit();
            this.getContainmentIndex().commitTransaction(id);
            this.getReferenceService().commitTransaction(id);
            this.getEventAccumulator().emitEvents(id, baseUri, userAgent);
            this.committed = true;
        } catch (final PersistentStorageException ex) {
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
            return null;
        });
        execQuietly("Failed to rollback index in transaction " + id, () -> {
            this.getContainmentIndex().rollbackTransaction(id);
            return null;
        });
        execQuietly("Failed to rollback reference index in transaction " + id, () -> {
            this.getReferenceService().rollbackTransaction(id);
            return null;
        });
        execQuietly("Failed to rollback events in transaction " + id, () -> {
            this.getEventAccumulator().clearEvents(id);
            return null;
        });
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

    /**
     * Executes the closure, capturing all exceptions, and logging them as errors.
     *
     * @param failureMessage what to print if the closure fails
     * @param callable closure to execute
     */
    private void execQuietly(final String failureMessage, final Callable<Void> callable) {
        try {
            callable.call();
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

}
