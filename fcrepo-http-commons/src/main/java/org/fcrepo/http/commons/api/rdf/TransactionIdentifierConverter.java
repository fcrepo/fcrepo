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
package org.fcrepo.http.commons.api.rdf;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.fcrepo.kernel.modeshape.services.TransactionServiceImpl.getCurrentTransactionId;

import javax.jcr.Session;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.modeshape.identifiers.IdentifierConverter;

/**
 * Translate the current transaction into the identifier
 * @author barmintor
 */
public class TransactionIdentifierConverter extends IdentifierConverter<String, String> {
    public static final String TX_PREFIX = "tx:";

    private final Session session;

    /**
     * 
     * @param session
     */
    public TransactionIdentifierConverter(final Session session) {
        this.session = session;
    }

    @Override
    public String apply(final String path) {

        if (path.contains(TX_PREFIX) && !path.contains(txSegment())) {
            throw new RepositoryRuntimeException("Path " + path
                    + " is not in current transaction " +  getCurrentTransactionId(session));
        }

        return replaceOnce(path, txSegment(), EMPTY);
    }

    @Override
    public String toDomain(final String path) {
        final String txSegment = txSegment();
        if (path.startsWith(txSegment)) {
            return path;
        }
        return txSegment + path;
    }

    private String txSegment() {

        final String txId = getCurrentTransactionId(session);

        if (txId != null) {
            return "/" + TX_PREFIX + txId;
        }
        return EMPTY;
    }

    @Override
    public String asString(final String resource) {
        return apply(resource).toString();
    }
}