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

import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.api.CommitOption.UNVERSIONED;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;

import edu.wisc.library.ocfl.api.MutableOcflRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * A default implemenntation of the {@link org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory} interface.
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class DefaultOCFLObjectSessionFactory implements OCFLObjectSessionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOCFLObjectSessionFactory.class);

    /**
     * Controls whether changes are committed to new OCFL versions or to a mutable HEAD
     */
    @Value("${fcrepo.autoversioning.enabled:true}")
    private boolean autoVersioningEnabled;

    private final Path ocflStagingDir;

    @Inject
    private MutableOcflRepository ocflRepository;

    /**
     * Constructor
     *
     * @param ocflStagingDir     The OCFL staging directory
     */
    @Autowired
    public DefaultOCFLObjectSessionFactory(
            @Value("#{ocflPropsConfig.fedoraOcflStaging}")
            final Path ocflStagingDir) {
        LOGGER.info("Fedora OCFL persistence staging directory:\n- {}",
                ocflStagingDir);

        try {
            this.ocflStagingDir = Files.createDirectories(ocflStagingDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public OCFLObjectSession create(final String ocflId, final String persistentStorageSessionId) {

        final Path stagingDirectory = this.ocflStagingDir.resolve(
                persistentStorageSessionId == null ? "read-only" : persistentStorageSessionId);
        return new DefaultOCFLObjectSession(ocflId, stagingDirectory,
                this.ocflRepository, defaultCommitOption());
    }

    private CommitOption defaultCommitOption() {
        if (autoVersioningEnabled) {
            return NEW_VERSION;
        }
        return UNVERSIONED;
    }

    /**
     * Enable or disable auto versioning on future sessions the factory creates
     *
     * @param autoVersioningEnabled true if auto versioning is enabled
     */
    public void setAutoVersioningEnabled(final boolean autoVersioningEnabled) {
        this.autoVersioningEnabled = autoVersioningEnabled;
    }

}
