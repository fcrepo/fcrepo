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


import static java.time.Duration.ofMinutes;

import java.time.Duration;
import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fedora Transaction implementation
 *
 * @author mohideen
 */
public class TransactionImpl implements Transaction {

    private final static Logger log = LoggerFactory.getLogger(TransactionImpl.class);

    final String id;

    final TransactionManagerImpl txManager;

    final Duration DEFAULT_TIMEOUT = ofMinutes(3);

    boolean shortLived = true;

    Instant expiration;

    boolean expired = false;

    boolean rolledback = false;

    boolean commited = false;

    protected TransactionImpl(final String id, final TransactionManagerImpl txManager) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Transaction id should not be empty!");
        }
        this.id = id;
        this.txManager = txManager;
        this.expiration = Instant.now().plus(timeout());
    }

    @Override
    public void commit() {
        failIfExpired();
        failIfRolledback();
        if(this.commited) {
            return;
        }
        try {
            log.debug("Commiting transaction {}", id);
            this.getPersistentSession().commit();
            this.commited = true;
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException("failed to commit transaction " + id, ex);
        }
    }

    @Override
    public void rollback() {
        failIfCommited();
        if(this.rolledback) {
            return;
        }
        try {
            log.debug("Rolling back transaction {}", id);
            this.getPersistentSession().rollback();
            this.rolledback = true;
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException("failed to rollback transaction " + id, ex);
        }
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
    public Instant updateExpiry(final Duration amountToAdd) {
        failIfExpired();
        failIfCommited();
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

    private Duration timeout() {
        // TODO Get the user configured timeout?
        // Otherwise, use the default timeout
        return DEFAULT_TIMEOUT;
    }

    private PersistentStorageSession getPersistentSession() {
        return this.txManager.getPersistentStorageSessionManager().getSession(this.id);
    }

    private void failIfExpired() {
        if(hasExpired()) {
            throw new RuntimeException("Transaction with transactionId: " + id + " expired!");
        }
    }

    private void failIfCommited() {
        if(this.commited) {
            throw new RuntimeException("Transaction with transactionId: " + id + " is already committed!");
        }
    }

    private void failIfRolledback() {
        if(this.rolledback) {
            throw new RuntimeException("Transaction with transactionId: " + id + " is already rolledback!");
        }
    }
}
