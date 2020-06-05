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

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageRuntimeException;
import org.fcrepo.persistence.common.ResourceHeaderSerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.library.ocfl.api.OcflObjectUpdater;
import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.exception.FixityCheckException;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;

/**
 * File visitor which walks a staging path in order to add content files to an OcflObjectUpdater.
 * If present, a transmission digest of the same algorithm as is used by the object for content
 * addressing will be provided for each binary.
 *
 * @author bbpennel
 */
public class OcflContentStagingFileVisitor implements FileVisitor<Path> {
    private static final Logger log = LoggerFactory.getLogger(OcflContentStagingFileVisitor.class);

    private final Path basePath;
    private final OcflObjectUpdater updater;
    private final DIGEST_ALGORITHM fcrepoDigestAlg;
    private final DigestAlgorithm ocflDigestAlg;
    private final OcflOption[] ocflOptions;

    public OcflContentStagingFileVisitor(final Path basePath, final OcflObjectUpdater updater,
            final DIGEST_ALGORITHM fcrepoDigestAlg, final DigestAlgorithm ocflDigestAlg,
            final OcflOption... ocflOptions) {
        this.basePath = basePath;
        this.updater = updater;
        this.fcrepoDigestAlg = fcrepoDigestAlg;
        this.ocflDigestAlg = ocflDigestAlg;
        this.ocflOptions = ocflOptions;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        final String subpath = getSubpath(dir).toString();
        if (OCFLPersistentStorageUtils.INTERNAL_FEDORA_DIRECTORY.equals(subpath)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final Path relPath = getSubpath(file);
        final String subpath = relPath.toString();

        try {
            updater.addPath(file, subpath, ocflOptions);

            final var sidecarPath = resolveStagedPath(getSidecarSubpath(subpath.toString()));
            final var resourceHeaders = ResourceHeaderSerializationUtils.deserializeHeaders(
                    Files.newInputStream(sidecarPath));
            final URI ocflDigest = resourceHeaders.getDigests().stream()
                    .filter(d -> fcrepoDigestAlg.algorithm.equals(ContentDigest.getAlgorithm(d)))
                    .findFirst()
                    .orElse(null);

            if (ocflDigest == null) {
                return FileVisitResult.CONTINUE;
            }
            final var digestValue = substringAfterLast(ocflDigest.toString(), ":");
            updater.addFileFixity(subpath, ocflDigestAlg, digestValue);
        } catch (final NoSuchFileException e) {
            // Sidecar file would not be found for rdf sources
            log.debug("Unable to find staged sidecar file for {}", basePath);
        } catch (final PersistentStorageException | IOException e) {
            throw new PersistentStorageRuntimeException("Failed to read sidecar file for " + file);
        } catch (final FixityCheckException e) {
            throw new PersistentStorageRuntimeException("Transmission of file " + file
                    + " failed due to fixity check failure: " + e.getMessage());
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
       return FileVisitResult.CONTINUE;
    }

    private Path getSubpath(final Path path) {
        return basePath.relativize(path);
    }

    private Path resolveStagedPath(final String encodedSubpath) {
        return basePath.resolve(encodedSubpath);
    }
}
