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

import org.fcrepo.kernel.api.FedoraTransaction;

/**
 * Provide a batch-aware HTTP session
 * @author acoburn
 */
public class HttpSession {

    private boolean batch = false;

    private final FedoraTransaction transaction;

    /**
     * Create an HTTP session from a Fedora transaction
     * @param transaction the Fedora transaction
     * Note: by default, the HTTP Session is not marked as a batch operation.
     * Client code must call makeBatch in order to promote the session into
     * a batch operation.
     */
    public HttpSession(final FedoraTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Commit a non-batch session
     */
    public void commit() {
        if (!isBatchSession()) {
            transaction.commit();
        }
    }

    /**
     * Expire a non-batch session
     */
    public void expire() {
        if (!isBatchSession()) {
            // Mark FedoraTransaction as expired?
        }
    }

    /**
     * Return whether this session is part of a batch operation
     * @return whether this session is part of a batch operation
     */
    public boolean isBatchSession() {
        return batch;
    }

    /**
     * Return the id of the underlying session
     * @return the session identifier
     */
    public String getId() {
        return transaction.getId();
    }

    /**
     * Return the underlying Fedoratransaction
     * @return the Fedoratransaction
     */
    public FedoraTransaction getFedoraTransaction() {
        return transaction;
    }
}
