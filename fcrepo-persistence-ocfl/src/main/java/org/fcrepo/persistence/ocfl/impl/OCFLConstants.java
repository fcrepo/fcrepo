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

import java.io.File;
import java.nio.file.Paths;

import static java.lang.System.getProperty;
import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;

/**
 * OCFL Constants.
 * @author whikloj
 * @author awoods
 * @since 6.0.0
 */
public final class OCFLConstants {

    private static final String OCFL_STAGING_DIR_KEY = "fcrepo.ocfl.staging.dir";
    public static final String OCFL_STORAGE_ROOT_DIR_KEY = "fcrepo.ocfl.storage.root.dir";
    public static final String OCFL_WORK_DIR_KEY = "fcrepo.ocfl.work.dir";
    private static final String FEDORA_TO_OCFL_INDEX_FILENAME = "fedoraToOcflIndex.tsv";

    /**
     * Return the system property key path as file or a file of the temporary directory + "system property key"
     * @param systemPropertyKey The system property
     * @return The file
     */
    private File resolveDir(final String systemPropertyKey) {
        final String path = getProperty(systemPropertyKey);
        if (path != null) {
            return new File(path);
        } else {
            //return default
            return Paths.get(JAVA_IO_TMPDIR, systemPropertyKey).toFile();
        }
    }

    /**
     * OCFL Staging directory
     * @return OCFL Staging directory
     */
    public File getStagingDir() {
        return resolveDir(OCFL_STAGING_DIR_KEY);
    }

    /**
     * OCFL Storage Root directory
     * @return OCFL Storage Root directory
     */
    public File getStorageRootDir() {
        return resolveDir(OCFL_STORAGE_ROOT_DIR_KEY);
    }

    /**
     * OCFL Work directory
     * @return OCFL Work directory
     */
    public File getWorkDir() {
        return resolveDir(OCFL_WORK_DIR_KEY);
    }

    /**
     * Fedora to OCFL index file
     * @return Fedora to OCFL index file
     */
    public File getFedoraToOCFLIndexFile() {
        return new File(getWorkDir() + File.separator +  FEDORA_TO_OCFL_INDEX_FILENAME);
    }
}
