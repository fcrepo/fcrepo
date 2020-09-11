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

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.NotFoundException;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflVersionInfo;
import org.fcrepo.storage.ocfl.ResourceContent;
import org.fcrepo.storage.ocfl.ResourceHeaders;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Wrapper around an OcflObjectSession to convert exceptions into fcrepo exceptions
 *
 * @author pwinckles
 */
public class FcrepoOcflObjectSessionWrapper implements OcflObjectSession {

    private final OcflObjectSession inner;

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
    public void writeResource(final ResourceHeaders headers, final InputStream content) {
        exec(() -> inner.writeResource(headers, content));
    }

    @Override
    public void deleteContentFile(final ResourceHeaders headers) {
        exec(() -> inner.deleteContentFile(headers));
    }

    @Override
    public void deleteResource(final String resourceId) {
        exec(() -> inner.deleteResource(resourceId));
    }

    @Override
    public boolean containsResource(final String resourceId) {
        return exec(() -> inner.containsResource(resourceId));
    }

    @Override
    public ResourceHeaders readHeaders(final String resourceId) {
        return exec(() -> inner.readHeaders(resourceId));
    }

    @Override
    public ResourceHeaders readHeaders(final String resourceId, final String versionNumber) {
        return exec(() -> inner.readHeaders(resourceId, versionNumber));
    }

    @Override
    public ResourceContent readContent(final String resourceId) {
        return exec(() -> inner.readContent(resourceId));
    }

    @Override
    public ResourceContent readContent(final String resourceId, final String versionNumber) {
        return exec(() -> inner.readContent(resourceId, versionNumber));
    }

    @Override
    public List<OcflVersionInfo> listVersions(final String resourceId) {
        return exec(() -> inner.listVersions(resourceId));
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
    public void commitType(final CommitType commitType) {
        inner.commitType(commitType);
    }

    @Override
    public void commit() {
        exec(inner::commit);
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
        } catch (NotFoundException e) {
            throw new PersistentItemNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new PersistentStorageException(e);
        }
    }

    private void exec(final VoidCallable callable) throws PersistentStorageException {
        try {
            callable.call();
        } catch (NotFoundException e) {
            throw new PersistentItemNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new PersistentStorageException(e);
        }
    }

    private interface VoidCallable {
        void call() throws Exception;
    }

}
