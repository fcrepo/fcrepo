/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api;

import java.time.Duration;
import java.time.Instant;

import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * A read-only tx that never expires and cannot be committed.
 *
 * @author pwinckles
 */
public class ReadOnlyTransaction implements Transaction {

    // This is not a perfect way to create a singleton, but it should be fine for its purposes as it does
    // not really need to be a singleton.
    public static final ReadOnlyTransaction INSTANCE = new ReadOnlyTransaction();

    public static final String READ_ONLY_TX_ID = "read-only";

    private ReadOnlyTransaction() {
        // empty
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
        return true;
    }

    @Override
    public void ensureCommitting() {
        // no-op
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getId() {
        return READ_ONLY_TX_ID;
    }

    @Override
    public boolean isShortLived() {
        return true;
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
        return Instant.now().plus(amountToAdd);
    }

    @Override
    public Instant getExpires() {
        return Instant.now().plus(Duration.ofMinutes(1));
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
    public void doInTx(final Runnable runnable) {
        runnable.run();
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
}
