/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.impl.utils;

import java.time.Duration;
import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * A test transaction implementation for Search Index
 *
 * @author whikloj
 */
public class TestTransaction implements Transaction {

    private final String id;

    private final boolean shortLived;

    public TestTransaction(final String txId, final boolean shortLived) {
        this.id = txId;
        this.shortLived = shortLived;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isShortLived() {
        return shortLived;
    }

    @Override
    public void setShortLived(final boolean shortLived) {
        // no-op
    }

    @Override
    public void expire() {
        // no-op
    }

    @Override
    public boolean hasExpired() {
        return false;
    }

    @Override
    public Instant updateExpiry(final Duration amountToAdd) {
        return null;
    }

    @Override
    public Instant getExpires() {
        return null;
    }

    @Override
    public void refresh() {
        // no-op
    }

    @Override
    public void lockResource(final FedoraId resourceId) {
        // no-op
    }

    @Override
    public void lockResourceNonExclusive(final FedoraId resourceId) {
        // no-op
    }

    @Override
    public void lockResourceAndGhostNodes(final FedoraId resourceId) {
        // no-op
    }

    @Override
    public void releaseResourceLocksIfShortLived() {
        // no-op
    }

    @Override
    public void doInTx(final Runnable closure) {
        closure.run();
    }

    @Override
    public void setBaseUri(final String baseUri) {
        // no-op
    }

    @Override
    public void setUserAgent(final String userAgent) {
        // no-op
    }

    @Override
    public void suppressEvents() {
        // no-op
    }

    @Override
    public void commit() {
        // no-op
    }

    @Override
    public void commitIfShortLived() {
        // no-op
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void rollback() {
        // no-op
    }

    @Override
    public void fail() {
        // no-op
    }

    @Override
    public boolean isRolledBack() {
        return false;
    }

    @Override
    public boolean isOpenLongRunning() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void ensureCommitting() {
        // no-op
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

}
