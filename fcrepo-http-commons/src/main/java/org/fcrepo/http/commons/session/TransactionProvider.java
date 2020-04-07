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

import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;

/**
 * Provide a fedora tranasction within the current request context
 *
 * @author awoods
 */
@Provider
@RequestScoped
public class TransactionProvider implements Factory<Transaction> {

    public static final Pattern TX_ID_PATTERN = Pattern.compile(".*/" + TX_PREFIX + "([0-9a-f\\-]+)$");

    @Inject
    private TransactionManager txManager;

    private final HttpServletRequest request;

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
        String txId = null;
        // Transaction id either comes from header or is the path
        String txUri = request.getHeader(ATOMIC_ID_HEADER);
        if (StringUtils.isEmpty(txUri)) {
            txUri = request.getPathInfo();
        }

        // Pull the id portion out of the tx uri
        if (!StringUtils.isEmpty(txUri)) {
            final Matcher txMatcher = TX_ID_PATTERN.matcher(txUri);
            if (txMatcher.matches()) {
                txId = txMatcher.group(1);
            }
        }

        if (!StringUtils.isEmpty(txId)) {
            return txManager.get(txId);
        } else {
            return txManager.create();
        }
    }
}
