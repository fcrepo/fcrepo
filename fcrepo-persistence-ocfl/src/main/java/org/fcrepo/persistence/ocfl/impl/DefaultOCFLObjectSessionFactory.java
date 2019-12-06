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

import static java.lang.System.getProperty;
import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;

import java.io.File;
import java.nio.file.Paths;

import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.layout.config.DefaultLayoutConfig;
import edu.wisc.library.ocfl.core.storage.FileSystemOcflStorage;
import org.springframework.stereotype.Component;

/**
 * A default implemenntation of the {@link org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory} interface.
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class DefaultOCFLObjectSessionFactory implements OCFLObjectSessionFactory {

    private static final File STAGING_DIR = resolveDir("fcrepo.ocfl.staging.dir", "fcrepo-ocfl-staging");
    private static final File OCFL_STORAGE_ROOT_DIR = resolveDir("fcrepo.ocfl.storage.root.dir", "fcrepo-ocfl");
    private static final File OCFL_WORK_DIR = resolveDir("fcrepo.ocfl.work.dir", "fcrepo-ocfl-work");

    private File ocflStagingDir;

    private MutableOcflRepository ocflRepository;

    private static final File resolveDir(final String systemPropertyKey, final String defaultDirectoryName) {
        final String path = getProperty(systemPropertyKey);
        if (path != null) {
            return new File(path);
        } else {
            //return default
            return Paths.get(getProperty(JAVA_IO_TMPDIR), defaultDirectoryName).toFile();
        }
    }

    /**
     * Default Constructor.  You can set the ocfl staging, storage root, and work directories by setting the following
     * system properties: fcrepo.ocfl.staging.dir, fcrepo.ocfl.storage.root.dir, and  fcrepo.ocfl.work.dir.  If these
     * are not set, default directories will be created in java.io.tmpdir.
     */
    public DefaultOCFLObjectSessionFactory() {
        this(STAGING_DIR, OCFL_STORAGE_ROOT_DIR, OCFL_WORK_DIR);
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

        this.ocflStagingDir = ocflStagingDir;
        this.ocflRepository = new OcflRepositoryBuilder()
                .layoutConfig(DefaultLayoutConfig.nTupleHashConfig())
                .buildMutable(FileSystemOcflStorage.builder().build(
                        ocflStorageRootDir.toPath()), ocflWorkDir.toPath());

    }

    @Override
    public OCFLObjectSession create(final String ocflId, final String persistentStorageSessionId) {
        final File stagingDirectory = new File(this.ocflStagingDir, persistentStorageSessionId);
        stagingDirectory.mkdirs();
        return new DefaultOCFLObjectSession(ocflId, stagingDirectory.toPath(), this.ocflRepository);
    }
}
