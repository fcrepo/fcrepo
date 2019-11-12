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
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.mapping.ObjectIdPathMapperBuilder;
import edu.wisc.library.ocfl.core.storage.FileSystemOcflStorage;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;

import java.io.File;

import static java.lang.System.getProperty;

/**
 * A default implemenntation of the {@link org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory} interface.
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class DefaultOCFLObjectSessionFactory implements OCFLObjectSessionFactory {

    private static final File DEFAULT_STAGING_DIR = new File(getProperty("fcrepo.ocfl.staging.dir",
            getProperty("java.io.tmpdir") + File.separator + "fcrepo-ocfl-staging"));
    private static final File DEFAULT_OCFL_STORAGE_ROOT_DIR = new File(getProperty("fcrepo.ocfl.storage.root.dir",
            getProperty("java.io.tmpdir") + File.separator + "fcrepo-ocfl"));
    private static final File DEFAULT_OCFL_WORK_DIR = new File(getProperty("fcrepo.ocfl.work.dir",
            getProperty("java.io.tmpdir") + File.separator + "fcrepo-ocfl-work"));

    private File ocflStagingRoot;

    private MutableOcflRepository ocflRepository;

    /**
     * Default Constructor.  You can set the ocfl staging, storage root, and work directories by setting the following
     * system properties: fcrepo.ocfl.staging.dir, fcrepo.ocfl.storage.root.dir, and  fcrepo.ocfl.work.dir.  If these
     * are not set, default directories will be created in java.io.tmpdir.
     */
    public DefaultOCFLObjectSessionFactory() {
        this(DEFAULT_STAGING_DIR, DEFAULT_OCFL_STORAGE_ROOT_DIR, DEFAULT_OCFL_WORK_DIR);
    }

    /**
     * Constructor
     *
     * @param ocflStagingDir     The OCFL staging directory
     * @param ocflStorageRootDir The OCFL storage root directory
     * @param ocflWorkDir        The OCFL work directory
     */
    public DefaultOCFLObjectSessionFactory(final File ocflStagingDir, final File ocflStorageRootDir,
                                           final File ocflWorkDir) {
        ocflStagingDir.mkdirs();
        ocflStorageRootDir.mkdirs();
        ocflWorkDir.mkdirs();

        this.ocflStagingRoot = ocflStagingRoot;
        this.ocflRepository = new OcflRepositoryBuilder().buildMutable(
                new FileSystemOcflStorage(ocflStorageRootDir.toPath(),
                        new ObjectIdPathMapperBuilder().buildFlatMapper()),
                ocflWorkDir.toPath());

    }

    @Override
    public OCFLObjectSession create(final String ocflId, final String persistentStorageSessionId) {
        final File stagingDirectory = new File(this.ocflStagingRoot, persistentStorageSessionId);
        stagingDirectory.mkdirs();
        return new DefaultOCFLObjectSession(ocflId, stagingDirectory.toPath(), this.ocflRepository);
    }
}
