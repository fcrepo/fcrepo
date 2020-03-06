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
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.STAGING_DIR;

import java.io.File;

import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;

import edu.wisc.library.ocfl.api.MutableOcflRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Controls whether or changes are committed to new OCFL versions or to a mutable HEAD
     */
    @Value("${fcrepo.autoversioning.enabled:false}")
    private boolean autoVersioningEnabled;

    private File ocflStagingDir;

    @Inject
    private MutableOcflRepository ocflRepository;

    /**
     * Default Constructor.  You can set the ocfl staging, storage root, and work directories by setting the following
     * system properties: fcrepo.ocfl.staging.dir, fcrepo.ocfl.storage.root.dir, and  fcrepo.ocfl.work.dir.  If these
     * are not set, default directories will be created in java.io.tmpdir.
     */
    public DefaultOCFLObjectSessionFactory() {
        this(STAGING_DIR);
    }

    /**
     * Constructor
     *
     * @param ocflStagingDir     The OCFL staging directory
     */
    public DefaultOCFLObjectSessionFactory(final File ocflStagingDir) {
        LOGGER.info("Fedora OCFL persistence staging directory:\n- {}",
                ocflStagingDir);

        ocflStagingDir.mkdirs();
        this.ocflStagingDir = ocflStagingDir;
    }

    @Override
    public OCFLObjectSession create(final String ocflId, final String persistentStorageSessionId) {

        final File stagingDirectory = new File(this.ocflStagingDir,
                persistentStorageSessionId == null ? "read-only" : persistentStorageSessionId);
        return new DefaultOCFLObjectSession(ocflId, stagingDirectory.toPath(),
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
