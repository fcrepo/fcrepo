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
import java.util.Optional;

import org.fcrepo.kernel.api.Transaction;

/**
 * The Fedora Transaction implementation
 *
 * @author mohideen
 */
public class TransactionImpl implements Transaction {

    final String id;

    boolean shortLived = true;

    TransactionImpl(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Transaction id should not be empty!");
        }
        this.id = id;
    }

    @Override
    public void commit() {
        // Prepare Persistence Transactions
        // Commit Persistence Transactions
    }

    @Override
    public void rollback() {
        // Rollback Persistence Transactions

        // Delete Transaction from TransactionManager state?
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Set transaction short-lived state.
     * 
     * @param shortLived boolean true (short-lived - default) or false (not short-lived)
     */
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
        // TODO Auto-generated method stub

    }

    @Override
    public Instant updateExpiry(final Duration amountToAdd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<Instant> getExpires() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void commitIfShortLived() {
       if (this.isShortLived()) {
           this.commit();
       }
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub
    }

}
