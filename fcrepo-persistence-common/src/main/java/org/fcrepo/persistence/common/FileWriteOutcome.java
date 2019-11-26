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
package org.fcrepo.persistence.common;

import static java.nio.file.Files.getLastModifiedTime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.persistence.api.WriteOutcome;


/**
 * Outcome information from writing a file.
 *
 * @author bbpennel
 */
public class FileWriteOutcome implements WriteOutcome {

    private Path filePath;

    private Collection<URI> digests;

    /**
     * Construct outcome
     *
     * @param filePath path of file written
     */
    public FileWriteOutcome(final Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Long getContentSize() {
        try {
            return Files.size(filePath);
        } catch (final IOException e) {
            throw new RepositoryRuntimeException("Unable to read file information for " + filePath, e);
        }
    }

    @Override
    public Instant getTimeWritten() {
        try {
            return getLastModifiedTime(filePath).toInstant();
        } catch (final IOException e) {
            throw new RepositoryRuntimeException("Unable to read file information for " + filePath, e);
        }
    }

    @Override
    public Collection<URI> getDigests() {
        return digests;
    }
}
