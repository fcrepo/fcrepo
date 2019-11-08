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

import static edu.wisc.library.ocfl.api.OcflOption.OVERWRITE;
import static edu.wisc.library.ocfl.api.OcflOption.MOVE_SOURCE;
import static org.fcrepo.persistence.ocfl.api.CommitOption.NEW_VERSION;
import static java.lang.String.format;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;

import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflObjectUpdater;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.model.CommitInfo;

/**
 * Default implementation of an OCFL object session, which stages changes to the
 * file system prior to committing.
 *
 * @author bbpennel
 */
public class DefaultOCFLObjectSession implements OCFLObjectSession {

    private String objectIdentifier;

    // Path where changes to the OCFL object in this session are staged
    private Path stagingPath;

    private Set<String> deletePaths;

    private MutableOcflRepository ocflRepository;

    /**
     * Instantiate an OCFL object session
     *
     * @param objectIdentifier identifier for the OCFL object
     * @param stagingPath path in which changes to the OCFL object will be staged.
     * @param ocflRepository the OCFL repository in which the object is stored.
     */
    public DefaultOCFLObjectSession(final String objectIdentifier, final Path stagingPath,
            final MutableOcflRepository ocflRepository) {
        this.objectIdentifier = objectIdentifier;
        this.stagingPath = stagingPath;
        this.ocflRepository = ocflRepository;
        this.deletePaths = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String subpath, final InputStream stream) throws PersistentStorageException {
        // Determine the staging path for the incoming content
        final var stagedPath = resolveStagedPath(subpath);
        final var parentPath = stagedPath.getParent();

        try {
            // Fill in any missing parent directories
            Files.createDirectories(parentPath);
            // write contents to subpath within the staging path
            Files.copy(stream, stagedPath, StandardCopyOption.REPLACE_EXISTING);
            stream.close();
        } catch (final IOException e) {
            throw new PersistentStorageException("Unable to persist content to " + stagedPath, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * If the file was not newly created within this session, then the
     * deletion will be recorded for replay at commit time.
     */
    @Override
    public void delete(final String subpath) throws PersistentStorageException {
        final var stagedPath = resolveStagedPath(subpath);
        final var hasStagedChanges = hasStagedChanges(stagedPath);

        if (hasStagedChanges) {
            // delete the file from the staging path
            try {
                Files.delete(stagedPath);
            } catch (final IOException e) {
                throw new PersistentStorageException("Unable to delete " + stagedPath, e);
            }
        }

        // for file that existed before this session, queue up its deletion for commit time
        if (!newInSession(subpath)) {
            deletePaths.add(subpath);
        } else if (!hasStagedChanges) {
            // Doesn't exist in staging or head version, so file doesn't exist
            throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                    subpath, objectIdentifier));
        }
    }

    private boolean newInSession(final String subpath) {
        // If the object isn't created yet, then there is no history for the subpath
        if (!ocflRepository.containsObject(objectIdentifier)) {
            return true;
        }
        // determine if this subpath exists in the OCFL object
        return !ocflRepository.getObject(ObjectVersionId.head(objectIdentifier))
                .containsFile(subpath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath) throws PersistentItemNotFoundException {
        final var stagedPath = resolveStagedPath(subpath);

        if (hasStagedChanges(stagedPath)) {
            // prioritize read of the staged version of the file
            try {
                return new FileInputStream(stagedPath.toFile());
            } catch (final FileNotFoundException e) {
                throw new PersistentItemNotFoundException(format("Could not find %s within object %s",
                        subpath, objectIdentifier));
            }
        } else {
            // Fall back to the head version
            return readVersion(subpath, ObjectVersionId.head(objectIdentifier));
        }
    }

    private boolean hasStagedChanges(final Path path) {
        return path.toFile().exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath, final String version) throws PersistentItemNotFoundException {
        return readVersion(subpath, ObjectVersionId.version(objectIdentifier, version));
    }

    private InputStream readVersion(final String subpath, final ObjectVersionId version) throws PersistentItemNotFoundException {
        try {
            // read the head version of the file from the ocfl object
            final var file = ocflRepository.getObject(version)
                    .getFile(subpath);
            if (file == null) {
                throw new PersistentItemNotFoundException(format("Could not find %s within object %s version %s",
                        subpath, objectIdentifier, version.getVersionId()));
            }
            return file.getStream();
        } catch (final NotFoundException e) {
            throw new PersistentItemNotFoundException(format(
                    "Unable to read %s from object %s version %s, object was not found.",
                    subpath, objectIdentifier, version.getVersionId()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare() {
        // TODO check for conflicts and lock the object
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String commit(final CommitOption commitOption) {
        if (commitOption == null) {
            throw new IllegalArgumentException("Invalid commit option provided: " + commitOption);
        }
        // Determine if a new object needs to be created
        if (isNewObject()) {
            return commitNewObject(commitOption);
        } else {
            return commitUpdates(commitOption);
        }
    }

    private String commitNewObject(final CommitOption commitOption) {
        final var commitInfo = new CommitInfo().setMessage("initial commit");

        if (NEW_VERSION.equals(commitOption)) {
            // perform commit to new version
            return ocflRepository.putObject(ObjectVersionId.head(objectIdentifier),
                    stagingPath,
                    commitInfo,
                    MOVE_SOURCE)
                    .getVersionId()
                    .toString();
        } else {
            // perform commit to head version
            return ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier), commitInfo, updater -> {
                updater.addPath(stagingPath, "", MOVE_SOURCE);
            }).getVersionId().toString();
        }
    }

    private String commitUpdates(final CommitOption commitOption) {
        // Updater which pushes all updated files and then performs queued deletes
        final Consumer<OcflObjectUpdater> commitChangeUpdater = updater -> {
            updater.addPath(stagingPath, "", MOVE_SOURCE, OVERWRITE);
            deletePaths.forEach(updater::removeFile);
        };

        if (NEW_VERSION.equals(commitOption)) {
            // check if a mutable head exists for this object
            if (ocflRepository.hasStagedChanges(objectIdentifier)) {
                // Persist the current changes to the mutable head, and then commit the head as a version
                ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier),
                        new CommitInfo(),
                        commitChangeUpdater);
                return ocflRepository.commitStagedChanges(objectIdentifier, new CommitInfo())
                        .getVersionId().toString();
            } else {
                // Commit directly to a new version
                return ocflRepository.updateObject(ObjectVersionId.head(objectIdentifier),
                        new CommitInfo(),
                        commitChangeUpdater)
                        .getVersionId().toString();
            }
        } else {
            // perform commit to mutable head version
            return ocflRepository.stageChanges(ObjectVersionId.head(objectIdentifier),
                    new CommitInfo(),
                    commitChangeUpdater)
                    .getVersionId().toString();
        }
    }

    private boolean isNewObject() {
        return !ocflRepository.containsObject(objectIdentifier);
    }

    private Path resolveStagedPath(final String subpath) {
        return stagingPath.resolve(subpath);
    }
}
