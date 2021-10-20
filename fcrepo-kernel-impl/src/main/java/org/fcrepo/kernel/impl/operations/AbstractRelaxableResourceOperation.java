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
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RelaxableResourceOperation;

import java.time.Instant;

/**
 * Abstract operation for a relaxable resource operations
 * @author bbpennel
 */
public abstract class AbstractRelaxableResourceOperation extends AbstractResourceOperation
                                                         implements RelaxableResourceOperation {
    protected String lastModifiedBy;

    protected String createdBy;

    protected Instant lastModifiedDate;

    protected Instant createdDate;

    protected AbstractRelaxableResourceOperation(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    protected void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    protected void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    protected void setLastModifiedDate(final Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @param createdDate the createdDate to set
     */
    protected void setCreatedDate(final Instant createdDate) {
        this.createdDate = createdDate;
    }
}
