/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.persistence.ocfl.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.lang.CheckedRunnable;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflVersionInfo;
import org.fcrepo.storage.ocfl.ResourceContent;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Wrapper around an OcflObjectSession to convert exceptions into fcrepo exceptions and time operations
 *
 * @author pwinckles
 */
public class FcrepoOcflObjectSessionWrapper implements OcflObjectSession {

    private final OcflObjectSession inner;

    private static final String METRIC_NAME = "fcrepo.storage.ocfl.object";
    private static final String OPERATION = "operation";
    private static final Timer writeTimer = Metrics.timer(METRIC_NAME, OPERATION, "write");
    private static final Timer writeHeadersTimer = Metrics.timer(METRIC_NAME, OPERATION, "writeHeaders");
    private static final Timer deleteContentTimer = Metrics.timer(METRIC_NAME, OPERATION, "deleteContent");
    private static final Timer deleteResourceTimer = Metrics.timer(METRIC_NAME, OPERATION, "deleteResource");
    private static final Timer containsResourceTimer = Metrics.timer(METRIC_NAME, OPERATION, "containsResource");
    private static final Timer readHeadersTimer = Metrics.timer(METRIC_NAME, OPERATION, "readHeaders");
    private static final Timer readContentTimer = Metrics.timer(METRIC_NAME, OPERATION, "readContent");
    private static final Timer readRangeTimer = Metrics.timer(METRIC_NAME, OPERATION, "readRange");
    private static final Timer listVersionsTimer = Metrics.timer(METRIC_NAME, OPERATION, "listVersions");
    private static final Timer commitTimer = Metrics.timer(METRIC_NAME, OPERATION, "commit");

    /**
     * @param inner the session to wrap
     */
    public FcrepoOcflObjectSessionWrapper(final OcflObjectSession inner) {
        this.inner = inner;
    }

    @Override
    public String sessionId() {
        return inner.sessionId();
    }

    @Override
    public String ocflObjectId() {
        return inner.ocflObjectId();
    }

    @Override
    public ResourceHeaders writeResource(final ResourceHeaders headers, final InputStream content) {
        return MetricsHelper.time(writeTimer, () -> {
            return exec(() -> inner.writeResource(headers, content));
        });
    }

    @Override
    public void writeHeaders(final ResourceHeaders headers) {
        writeHeadersTimer.record(() -> {
            exec(() -> inner.writeHeaders(headers));
        });
    }

    @Override
    public void deleteContentFile(final ResourceHeaders headers) {
        deleteContentTimer.record(() -> {
            exec(() -> inner.deleteContentFile(headers));
        });
    }

    @Override
    public void deleteResource(final String resourceId) {
        deleteResourceTimer.record(() -> {
            exec(() -> inner.deleteResource(resourceId));
        });
    }

    @Override
    public boolean containsResource(final String resourceId) {
        return MetricsHelper.time(containsResourceTimer, () -> {
            return exec(() -> inner.containsResource(resourceId));
        });
    }

    @Override
    public ResourceHeaders readHeaders(final String resourceId) {
        return MetricsHelper.time(readHeadersTimer, () -> {
            return exec(() -> inner.readHeaders(resourceId));
        });
    }

    @Override
    public ResourceHeaders readHeaders(final String resourceId, final String versionNumber) {
        return MetricsHelper.time(readHeadersTimer, () -> {
            return exec(() -> inner.readHeaders(resourceId, versionNumber));
        });
    }

    @Override
    public ResourceContent readContent(final String resourceId) {
        return MetricsHelper.time(readContentTimer, () -> {
            return exec(() -> inner.readContent(resourceId));
        });
    }

    @Override
    public ResourceContent readContent(final String resourceId, final String versionNumber) {
        return MetricsHelper.time(readContentTimer, () -> {
            return exec(() -> inner.readContent(resourceId, versionNumber));
        });
    }

    @Override
    public ResourceContent readRange(final String resourceId, final long start, final long end) {
        return MetricsHelper.time(readRangeTimer, () ->
                exec(() -> inner.readRange(resourceId, start, end)));
    }

    @Override
    public ResourceContent readRange(final String resourceId, final String versionNumber,
                                     final long start, final long end) {
        return MetricsHelper.time(readRangeTimer, () ->
                exec(() -> inner.readRange(resourceId, versionNumber, start, end)));
    }

    @Override
    public List<OcflVersionInfo> listVersions(final String resourceId) {
        return MetricsHelper.time(listVersionsTimer, () -> {
            return exec(() -> inner.listVersions(resourceId));
        });
    }

    @Override
    public Stream<ResourceHeaders> streamResourceHeaders() {
        return exec(inner::streamResourceHeaders);
    }

    @Override
    public void versionCreationTimestamp(final OffsetDateTime timestamp) {
        inner.versionCreationTimestamp(timestamp);
    }

    @Override
    public void versionAuthor(final String name, final String address) {
        inner.versionAuthor(name, address);
    }

    @Override
    public void versionMessage(final String message) {
        inner.versionMessage(message);
    }

    @Override
    public void invalidateCache(final String resourceId) {
        inner.invalidateCache(resourceId);
    }

    @Override
    public void commitType(final CommitType commitType) {
        inner.commitType(commitType);
    }

    @Override
    public void commit() {
        commitTimer.record(() -> {
            exec(inner::commit);
        });
    }

    @Override
    public void rollback() {
        exec(inner::rollback);
    }

    @Override
    public void abort() {
        exec(inner::abort);
    }

    @Override
    public boolean isOpen() {
        return inner.isOpen();
    }

    @Override
    public void close() {
        exec(inner::close);
    }

    private <T> T exec(final Callable<T> callable) throws PersistentStorageException {
        try {
            return callable.call();
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new PersistentStorageException(e.getMessage(), e);
        }
    }

    private void exec(final CheckedRunnable runnable) throws PersistentStorageException {
        try {
            runnable.run();
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new PersistentStorageException(e.getMessage(), e);
        }
    }

}
