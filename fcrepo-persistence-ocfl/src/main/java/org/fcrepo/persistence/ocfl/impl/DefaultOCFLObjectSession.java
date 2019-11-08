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

import static org.fcrepo.persistence.ocfl.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.ocfl.api.CommitOption.MUTABLE_HEAD;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fcrepo.persistence.ocfl.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;

import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.MutableOcflRepository;

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
    }

    private Set<String> getDeletePaths() {
        if (deletePaths == null) {
            // TODO check to see if a delete log exists, in case resuming after reboot
            deletePaths = new HashSet<>();
        }
        return deletePaths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String subpath, final InputStream stream) {
        // TODO write contents to the subpath within the staging path

        // Determine the staging path for the incoming content
        resolveStagedPath(subpath);
    }

    /**
     * {@inheritDoc}
     *
     * If the file was not newly created within this session, then the
     * deletion will be recorded for replay at commit time.
     */
    @Override
    public void delete(final String subpath) {
        if (newInSession(subpath)) {
            // TODO delete the file from the staging path
        } else {
            getDeletePaths().add(subpath);
        }
    }

    /**
     * {@inheritDoc}
     */
    private boolean newInSession(final String subpath) {
        // TODO determine if this subpath exists in the OCFL object
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath) {
        if (hasStagedChanges(subpath)) {
            // TODO read the staged version of the file
        } else {
            // TODO read the head version of the file from the ocfl object
        }
        return null;
    }

    private boolean hasStagedChanges(final String subpath) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(final String subpath, final String version) {
        final AtomicReference<InputStream> contentRef = new AtomicReference<>();

        ocflRepository.readObject(ObjectVersionId.version(objectIdentifier, version), reader -> {
            contentRef.set(reader.getFile(subpath));
        });

        return contentRef.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare() {
        // TODO check for conflicts
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit(final CommitOption commitOption) {
        // Determine if a new object needs to be created
        if (isNewObject()) {
            // TODO create new object from the staged content
        } else {
            if (NEW_VERSION.equals(commitOption)) {
                // TODO perform commit to new version
            } else if (MUTABLE_HEAD.equals(commitOption)) {
                // TODO perform commit to head version
            } else {
                throw new IllegalArgumentException("Invalid commit option provided: " + commitOption);
            }
        }
    }

    private boolean isNewObject() {
        return false;
    }

    private Path resolveStagedPath(final String subpath) {
        return stagingPath.resolve(subpath);
    }
}
