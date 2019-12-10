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
package org.fcrepo.http.commons.session;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Provide a fedora tranasction within the current request context
 *
 * @author awoods
 */
@Provider
@RequestScoped
@Component
public class TransactionProvider implements Factory<Transaction> {

    @Inject
    private TransactionManager txManager;

    private final HttpServletRequest request;

    public static final String ATOMIC_ID_HEADER = "Atomic-ID";

    /**
     * Create a new transaction provider for a request
     * @param request the request
     */
    @Inject
    public TransactionProvider(final HttpServletRequest request) {
        this.request = request;
    }

    private static final Logger LOGGER = getLogger(TransactionProvider.class);

    @Override
    public Transaction provide() {
        final Transaction transaction = getTransactionForRequest(request);
        if (!transaction.isShortLived()) {
            transaction.refresh();
        }
        LOGGER.trace("Providing new transaction {}", transaction);
        return transaction;
    }

    @Override
    public void dispose(final Transaction transaction) {
        if (transaction.isShortLived()) {
            LOGGER.trace("Disposing transaction {}", transaction);
            transaction.expire();
        }
    }

    /**
     * Returns the transaction for the Request. If the request has ATOMIC_ID_HEADER header,
     * the transaction corresponding to that ID is returned, otherwise, a new transaction is
     * created.
     * 
     * @param request the request object
     * @return the transaction for the request
     */
    public Transaction getTransactionForRequest(final HttpServletRequest request) {
        final String txId = request.getHeader(ATOMIC_ID_HEADER);
        if (txId != null && !txId.isEmpty()) {
           return txManager.get(txId);
        } else {
           return txManager.create();
        }
    }
}
