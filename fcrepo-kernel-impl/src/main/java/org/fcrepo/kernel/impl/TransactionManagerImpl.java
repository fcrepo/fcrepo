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

import static java.util.UUID.randomUUID;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import java.util.HashMap;

import javax.inject.Inject;


/**
 * The Fedora Transaction Manager implementation
 *
 * @author mohideen
 */
public class TransactionManagerImpl implements TransactionManager {

    private final HashMap<String, Transaction> transactions;

    @Inject
    private static PersistentStorageSessionManager pSessionManager;

    TransactionManagerImpl() {
        transactions = new HashMap();
    }

    // TODO Add a timer to periadically rollback and cleanup expired transaction?

    @Override
    public synchronized Transaction create() {
        String txId = randomUUID().toString();
        while(transactions.containsKey(txId)) {
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
                transactions.remove(transactionId);
                throw new TransactionRuntimeException("Transaction with transactionId: " + transactionId +
                    " expired at " + transaction.getExpires() + "!");
            }
            return transaction;
        } else {
            throw new TransactionRuntimeException("No Transaction found with transactionId: " + transactionId);
        }
    }

    protected PersistentStorageSessionManager getPersistentStorageSessionManager() {
        return TransactionManagerImpl.pSessionManager;
    }
}
