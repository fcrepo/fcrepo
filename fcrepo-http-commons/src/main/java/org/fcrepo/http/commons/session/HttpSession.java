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

import org.fcrepo.kernel.api.FedoraSession;

/**
 * Provide a batch-aware HTTP session
 * @author acoburn
 */
public class HttpSession {

    private boolean batch = false;

    private final FedoraSession session;

    /**
     * Create an HTTP session from a Fedora session
     * @param session the Fedora session
     * Note: by default, the HTTP Session is not marked as a batch operation.
     * Client code must call makeBatch in order to promote the session into
     * a batch operation.
     */
    public HttpSession(final FedoraSession session) {
        this.session = session;
    }

    /**
     * Make this HTTP Session into a batch operation.
     */
    public void makeBatchSession() {
        this.batch = true;
    }

    /**
     * Commit a non-batch session
     */
    public void commit() {
        if (!isBatchSession()) {
            session.commit();
        }
    }

    /**
     * Expire a non-batch session
     */
    public void expire() {
        if (!isBatchSession()) {
            session.expire();
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
        return session.getId();
    }

    /**
     * Return the underlying FedoraSession
     * @return the FedoraSession
     */
    public FedoraSession getFedoraSession() {
        return session;
    }
}
