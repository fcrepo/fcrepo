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

import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;

import static java.lang.System.getProperty;

import java.io.File;
import java.nio.file.Paths;

/**
 * OCFL Constants.
 * @author whikloj
 * @since 6.0.0
 */
public final class OCFLConstants {

    public static final File STAGING_DIR = resolveDir("fcrepo.ocfl.staging.dir");
    public static final File OCFL_STORAGE_ROOT_DIR = resolveDir("fcrepo.ocfl.storage.root.dir");
    public static final File OCFL_WORK_DIR = resolveDir("fcrepo.ocfl.work.dir");
    public static final File FEDORA_TO_OCFL_INDEX_FILE = new File(OCFL_WORK_DIR.toString() + File.separator +
            "fedoraToOcflIndex.tsv");

    /**
     * Return the system property key path as file or a file of the temporary directory + "system property key"
     * @param systemPropertyKey The system property
     * @return The file
     */
    private static File resolveDir(final String systemPropertyKey) {
        final String path = getProperty(systemPropertyKey);
        if (path != null) {
            return new File(path);
        } else {
            //return default
            return Paths.get(JAVA_IO_TMPDIR, systemPropertyKey).toFile();
        }
    }

    private OCFLConstants() {
        // This method left intentionally blank.
    }
}
