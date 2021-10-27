/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
