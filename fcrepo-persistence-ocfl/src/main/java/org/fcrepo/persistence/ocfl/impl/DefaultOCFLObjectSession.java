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

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflObjectUpdater;
import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.exception.FixityCheckException;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.model.CommitInfo;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;
import edu.wisc.library.ocfl.api.model.FileChangeType;
import edu.wisc.library.ocfl.api.model.FileDetails;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionDetails;
import edu.wisc.library.ocfl.api.model.VersionId;
import edu.wisc.library.ocfl.core.util.FileUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageRuntimeException;
import org.fcrepo.persistence.common.FileWriteOutcome;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.wisc.library.ocfl.api.OcflOption.MOVE_SOURCE;
import static edu.wisc.library.ocfl.api.OcflOption.OVERWRITE;
import static java.lang.String.format;
import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.translateFedoraDigestToOcfl;

/**
 * Default implementation of an OCFL object session, which stages changes to the
 * file system prior to committing.
 *
 * @author bbpennel
 */
public class DefaultOCFLObjectSession implements OCFLObjectSession {

    private static final Logger log = LoggerFactory.getLogger(DefaultOCFLObjectSession.class);

    private final String objectIdentifier;

    // Path where changes to the OCFL object in this session are staged
    private final Path stagingPath;

    private Set<String> deletePaths;

    private boolean objectDeleted;

    // Indicates that the session has been committed or closed, and may not be written to
    private boolean sessionClosed;

    private final MutableOcflRepository ocflRepository;

    private CommitOption commitOption;

    private final Map<String, String> subpathToDigest;

    private final Instant created;
    /**
     * Instantiate an OCFL object session
     *
     * @param objectIdentifier identifier for the OCFL object
     * @param stagingPath path in which changes to the OCFL object will be staged.
     * @param ocflRepository the OCFL repository in which the object is stored.
     * @param commitOption the commit option to use
     */
    public DefaultOCFLObjectSession(final String objectIdentifier, final Path stagingPath,
            final MutableOcflRepository ocflRepository, final CommitOption commitOption) {
        this.objectIdentifier = objectIdentifier;
        this.stagingPath = stagingPath.resolve(createStagingDirectory(stagingPath, objectIdentifier));
        log.debug("OCFL object <{}> is staging files at <{}>", objectIdentifier, stagingPath);
        this.ocflRepository = ocflRepository;
        this.commitOption = commitOption;
        this.deletePaths = new HashSet<>();
        this.objectDeleted = false;
        this.sessionClosed = false;
        this.created = Instant.now();
        this.subpathToDigest = new HashMap<>();
    }

    private static Path createStagingDirectory(final Path sessionStaging, final String objectIdentifier) {
        final var digest = DigestUtils.sha256Hex(objectIdentifier);
        try {
            return Files.createDirectories(sessionStaging.resolve(digest));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String encode(final String value) {
        if (SystemUtils.IS_OS_WINDOWS) {
            final String encoded;
            if (value.contains("/")) {
                encoded = Arrays.stream(value.split("/")).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
                        .collect(Collectors.joining("/"));
            } else {
                encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
            }
            log.debug("Encoding {} to {}", value, encoded);
            return encoded;
        }
        return value;
    }

    private String decode(final String value) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized WriteOutcome write(final String subpath, final InputStream stream)
            throws PersistentStorageException {
        final var encodedSubpath = encode(subpath);

        // Check that the staging path exists now that we are writing.
        if (!this.stagingPath.toFile().exists()) {
            this.stagingPath.toFile().mkdirs();
        }
        // Determine the staging path for the incoming content
        final var stagedPath = resolveStagedPath(encodedSubpath);
        try {
            assertSessionOpen();

            final var parentPath = stagedPath.getParent();

            // Fill in any missing parent directories
            Files.createDirectories(parentPath);
            // write contents to subpath within the staging path
            Files.copy(stream, stagedPath, StandardCopyOption.REPLACE_EXISTING);

            return new FileWriteOutcome(stagedPath);
        } catch (final IOException e) {
            throw new PersistentStorageException("Unable to persist content to " + stagedPath, e);
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                log.error("Failed to close inputstream while writing {}", subpath, e);
            }
        }
    }

    @Override
    public void registerTransmissionDigest(final String subpath, final String digest) {
        final var encodedSubpath = encode(subpath);
        subpathToDigest.put(encodedSubpath, digest);
    }

    /**
     * {@inheritDoc}
     *
     * If the file was not newly created within this session, then the
     * deletion will be recorded for replay at commit time.
     */
    @Override
    public synchronized void delete(final String subpath) throws PersistentStorageException {
        assertSessionOpen();

        final var encodedSubpath = encode(subpath);

        final var stagedPath = resolveStagedPath(encodedSubpath);
        final var hasStagedChanges = hasStagedChanges(stagedPath);

        // If the subpath exists in the staging path for this session, then delete from there
        if (hasStagedChanges) {
            // delete the file from the staging path
            try {
                Files.delete(stagedPath);
            } catch (final IOException e) {
                throw new PersistentStorageException("Unable to delete " + stagedPath, e);
            }
        }

        // for file that existed before this session, queue up its deletion for commit time
        if (!isNewInSession(subpath)) {
            deletePaths.add(encodedSubpath);
        } else if (!hasStagedChanges) {
            // File is neither in the staged or exists in the head version, so file cannot be found for deletion
            if (objectDeleted && isStagingEmpty()) {
                // File can't be found because the object no longer exists
                throw new PersistentItemNotFoundException(format(
                        "Could not delete from object %s, it does not exist", objectIdentifier));
            } else {
                throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                        subpath, objectIdentifier));
            }
        }
    }

    @Override
    public synchronized void deleteObject() throws PersistentStorageException {
        assertSessionOpen();

        objectDeleted = true;
        // Reset state of the object
        if (!isStagingEmpty()) {
            cleanupStaging();
            deletePaths = new HashSet<>();
        } else if (isNewObject()) {
            // fail if attempting to delete object that does not exist and has no staged changes
            throw new PersistentItemNotFoundException(format(
                    "Cannot delete object %s, it does not exist", objectIdentifier));
        }
    }

    private void cleanupStaging() throws PersistentStorageException {
        try {
            final var stagingDir = stagingPath.toFile();
            if (stagingDir.exists()) {
                FileUtils.cleanDirectory(stagingDir);
            }
        } catch (final IOException e) {
            throw new PersistentStorageException("Unable to cleanup staging path", e);
        }
    }

    @Override
    public boolean isNewInSession(final String subpath) {

        final var encodedSubpath = encode(subpath);

        if (isNewInSession()) {
            return true;
        }

        // determine if this subpath exists in the OCFL object
        return !ocflRepository.getObject(ObjectVersionId.head(objectIdentifier))
                .containsFile(encodedSubpath);
    }

    @Override
    public boolean isNewInSession() {
        // If the object was deleted in this session, then content can only be new
        if (objectDeleted) {
            return true;
        }
        // If the object isn't created yet, then the object must be new.
        if (!ocflRepository.containsObject(objectIdentifier)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath) throws PersistentStorageException {
        assertSessionOpen();

        final var encodedSubpath = encode(subpath);

        final var stagedPath = resolveStagedPath(encodedSubpath);

        if (hasStagedChanges(stagedPath)) {
            // prioritize read of the staged version of the file
            try {
                return new FileInputStream(stagedPath.toFile());
            } catch (final FileNotFoundException e) {
                throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                        subpath, objectIdentifier), e);
            }
        } else if (!objectDeleted) {
            // Fall back to the head version
            return readVersion(subpath, ObjectVersionId.head(objectIdentifier));
        }

        throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                subpath, objectIdentifier));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath, final String version) throws PersistentStorageException {
        assertSessionOpen();

        // If the object was deleted, then only uncommitted staged files can be available
        if (objectDeleted) {
            throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                    subpath, objectIdentifier));
        }

        return readVersion(subpath, ObjectVersionId.version(objectIdentifier, version));
    }

    private InputStream readVersion(final String subpath, final ObjectVersionId version)
            throws PersistentItemNotFoundException {
        final var encodedSubpath = encode(subpath);

        try {
            // read the head version of the file from the ocfl object
            final var file = ocflRepository.getObject(version)
                    .getFile(encodedSubpath);
            if (file == null) {
                throw new PersistentItemNotFoundException(format("Could not find %s within object %s version %s",
                        subpath, objectIdentifier, version.getVersionId()));
            }
            // Disable automatic fixity check
            return file.getStream().enableFixityCheck(false);
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(format(
                    "Unable to read %s from object %s version %s, object was not found.",
                    subpath, objectIdentifier, version.getVersionId()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommitOption(final CommitOption commitOption) {
        this.commitOption = commitOption;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommitOption getCommitOption() {
        return commitOption;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void prepare() {
        // TODO check for conflicts and lock the object
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String commit() throws PersistentStorageException {
        assertSessionOpen();

        if (commitOption == null) {
            throw new IllegalArgumentException("Invalid commit option provided: " + commitOption);
        }

        // Close the session
        sessionClosed = true;

        // Perform requested deletion of the object
        if (objectDeleted) {
            deleteExistingObject();
            // no new state provided for the object, this is just committing the delete
            if (isStagingEmpty()) {
                return null;
            }
            // new state exists, object is being recreated after delete
        }

        // Determine if a new object needs to be created
        if (isNewObject()) {
            return commitNewObject(commitOption);
        } else {
            return commitUpdates(commitOption);
        }
    }

    private void deleteExistingObject() {
        ocflRepository.purgeObject(objectIdentifier);
    }

    private String commitNewObject(final CommitOption commitOption) throws PersistentStorageException {
        final var commitInfo = new CommitInfo().setMessage("initial commit");

        // Prevent creation of empty OCFL objects
        if (isStagingEmpty()) {
            throw new PersistentStorageException("Cannot create empty OCFL object " + objectIdentifier);
        }

        final Consumer<OcflObjectUpdater> commitChangeUpdater = constructCommitUpdater(false);

        if (NEW_VERSION.equals(commitOption)) {
            // perform commit to new version
            return ocflRepository.updateObject(ObjectVersionId.head(objectIdentifier), commitInfo, commitChangeUpdater)
                    .getVersionId().toString();
        } else {
            // perform commit to head version
            return ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier), commitInfo, commitChangeUpdater)
                    .getVersionId().toString();
        }
    }

    private String commitUpdates(final CommitOption commitOption) throws PersistentStorageException {
        // Nothing to do if there are no staged files, no deletes, and not committing the mutable HEAD
        if (isStagingEmpty() && deletePaths.isEmpty() &&
                !(commitOption == NEW_VERSION && ocflRepository.hasStagedChanges(objectIdentifier))) {
            return ocflRepository.describeVersion(ObjectVersionId.head(objectIdentifier))
                    .getVersionId().toString();
        }

        // Updater which pushes all updated files and then performs queued deletes
        final Consumer<OcflObjectUpdater> commitChangeUpdater = constructCommitUpdater(true);

        if (NEW_VERSION.equals(commitOption)) {
            // check if a mutable head exists for this object
            if (ocflRepository.hasStagedChanges(objectIdentifier)) {
                // Persist the current changes to the mutable head, and then commit the head as a version
                ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier),
                        null,
                        commitChangeUpdater);
                return ocflRepository.commitStagedChanges(objectIdentifier, null)
                        .getVersionId().toString();
            } else {
                // Commit directly to a new version
                return ocflRepository.updateObject(ObjectVersionId.head(objectIdentifier),
                        null,
                        commitChangeUpdater)
                        .getVersionId().toString();
            }
        } else {
            // perform commit to mutable head version
            return ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier),
                    null,
                    commitChangeUpdater)
                    .getVersionId().toString();
        }
    }

    private Consumer<OcflObjectUpdater> constructCommitUpdater(final boolean updatingObject) {
        final DIGEST_ALGORITHM fcrepoDigestAlg = getObjectDigestAlgorithm();
        final DigestAlgorithm ocflDigestAlg = translateFedoraDigestToOcfl(fcrepoDigestAlg);
        final OcflOption[] ocflOptions;
        if (updatingObject) {
            ocflOptions = new OcflOption[] { MOVE_SOURCE, OVERWRITE };
        } else {
            ocflOptions = new OcflOption[] { MOVE_SOURCE };
        }


        return updater -> {
            if (updatingObject) {
                deletePaths.forEach(updater::removeFile);
            }
            if (isStagingEmpty()) {
                return;
            }

            updater.addPath(stagingPath, ocflOptions);

            subpathToDigest.forEach((subpath, digest) -> {
                final String normalizedPath = FileUtil.pathToStringStandardSeparator(Paths.get(subpath));
                try {
                    updater.addFileFixity(normalizedPath, ocflDigestAlg, digest);
                } catch (final FixityCheckException e) {
                    throw new PersistentStorageRuntimeException("Transmission of file " + subpath
                            + " failed due to fixity check failure: " + e.getMessage());
                }
            });
        };
    }

    /**
     * {@inheritDoc}
     *
     * Close the session and perform cleanup.
     */
    @Override
    public synchronized void close() throws PersistentStorageException {
        sessionClosed = true;

        if (!FileUtils.deleteQuietly(stagingPath.toFile())) {
            log.warn("Failed to delete staging directory: {}", stagingPath);
        }
    }

    @Override
    public List<OCFLVersion> listVersions() throws PersistentStorageException {
        assertSessionOpen();
        //get a list of all versions in the object.
        try {
            return this.ocflRepository.describeObject(this.objectIdentifier)
                    .getVersionMap().values().stream()
                    // do not include mutable versions
                    .filter(version -> !version.isMutable())
                    // do not include empty v1s, they were likely created as a mutable HEAD placeholder
                    .filter(version -> !(VersionId.V1.equals(version.getVersionId()) && version.getFiles().isEmpty()))
                    .sorted(VERSION_COMPARATOR)
                    .map(version -> {
                        return new OCFLVersionImpl()
                                .setOcflObjectId(version.getObjectId())
                                .setOcflVersionId(version.getVersionId().toString())
                                .setCreatedBy(getCreatedBy(version.getCommitInfo()))
                                .setCreated(toMementoInstant(version.getCreated()));
                    }).collect(Collectors.toList());
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(format(
                    "Could not list versions, object %s was not found.",
                    objectIdentifier), e);
        }
    }

    @Override
    public List<OCFLVersion> listVersions(final String subpath) throws PersistentStorageException {
        if (StringUtils.isBlank(subpath)) {
            return listVersions();
        }

        assertSessionOpen();

        final var encodedSubpath = encode(subpath);

        try {
            final var headDesc = ocflRepository.describeVersion(ObjectVersionId.head(objectIdentifier));

            return ocflRepository.fileChangeHistory(objectIdentifier, encodedSubpath).getFileChanges().stream()
                    .filter(change -> change.getChangeType() == FileChangeType.UPDATE)
                    // do not include changes that were made in the mutable head
                    .filter(change -> !(headDesc.isMutable() && headDesc.getVersionId().equals(change.getVersionId())))
                    .map(change -> {
                        return new OCFLVersionImpl()
                                .setOcflObjectId(objectIdentifier)
                                .setOcflVersionId(change.getVersionId().toString())
                                .setCreatedBy(getCreatedBy(change.getCommitInfo()))
                                .setCreated(toMementoInstant(change.getTimestamp()));
                    }).collect(Collectors.toList());
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(format(
                    "Could not list versions, object %s subpath %s was not found.",
                    objectIdentifier, subpath), e);
        }
    }

    @Override
    public Stream<String> listHeadSubpaths() throws PersistentStorageException {
        assertSessionOpen();
        try {
            return this.ocflRepository.describeVersion(ObjectVersionId.head(this.objectIdentifier))
                    .getFiles()
                    .stream()
                    .map(FileDetails::getPath)
                    .map(this::decode);
        } catch (final NotFoundException exc) {
            return Stream.empty();
        }
    }

    @Override
    public Instant getCreated() {
        return this.created;
    }
    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private static class VersionComparator implements Comparator<VersionDetails> {
        @Override
        public int compare(final VersionDetails a, final VersionDetails b) {
            return a.getVersionId().compareTo(b.getVersionId());
        }
    }

    private boolean isStagingEmpty() {
        return !stagingPath.toFile().exists() || Objects.requireNonNull(stagingPath.toFile().listFiles()).length == 0;
    }

    private boolean hasStagedChanges(final Path path) {
        return path.toFile().exists();
    }

    private boolean isNewObject() {
        return !ocflRepository.containsObject(objectIdentifier);
    }

    private Path resolveStagedPath(final String encodedSubpath) {
        return stagingPath.resolve(encodedSubpath);
    }

    private void assertSessionOpen() throws PersistentSessionClosedException {
        if (sessionClosed) {
            throw new PersistentSessionClosedException("Cannot perform operation, session is closed");
        }
    }

    private String getCreatedBy(final CommitInfo commitInfo) {
        if (commitInfo != null) {
            final var user = commitInfo.getUser();
            if (user != null) {
                return user.getName();
            }
        }
        return null;
    }

    private Instant toMementoInstant(final OffsetDateTime timestamp) {
        return timestamp.toInstant().truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public DIGEST_ALGORITHM getObjectDigestAlgorithm() {
        if (isNewObject()) {
            return ContentDigest.DEFAULT_DIGEST_ALGORITHM;
        }
        final DigestAlgorithm ocflAlg = ocflRepository.describeObject(this.objectIdentifier).getDigestAlgorithm();
        final DIGEST_ALGORITHM fcrepoAlg = DIGEST_ALGORITHM.fromAlgorithm(ocflAlg.getJavaStandardName());
        if (DIGEST_ALGORITHM.MISSING.equals(fcrepoAlg)) {
            throw new UnsupportedAlgorithmException(String.format("OCFL object %s specifies a digest algorithm "
                    + "which is not supported by Fedora: %s", objectIdentifier, ocflAlg.getJavaStandardName()));
        }
        return fcrepoAlg;
    }

}
