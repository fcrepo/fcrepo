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
package org.fcrepo.persistence.ocfl;

import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_STAGING_PROPERTY;
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_STORAGE_ROOT_PROPERTY;
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_WORK_PROPERTY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

/**
 * Utility which sets up a temporary base directory for OCFL, establishes
 * system properties to make use of it, and cleans up on shutdown.
 *
 * @author bbpennel
 */
public class TemporaryOCFLRepositoryHelper {

    /**
     * Initialize the helper
     *
     * @throws IOException thrown if the base directory cannot be created
     */
    public TemporaryOCFLRepositoryHelper() throws IOException {
        final Path ocflBase = Files.createDirectory(Paths.get("target/ocfl_base"));
        final Path ocflStagingDir = ocflBase.resolve("staging");
        final Path ocflStorageRootDir = ocflBase.resolve("storage_root");
        final Path ocflWorkDir = ocflBase.resolve("work");

        System.setProperty(OCFL_STAGING_PROPERTY, ocflStagingDir.toString());
        System.setProperty(OCFL_STORAGE_ROOT_PROPERTY, ocflStorageRootDir.toString());
        System.setProperty(OCFL_WORK_PROPERTY, ocflWorkDir.toString());

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                FileUtils.deleteQuietly(ocflBase.toFile())));
    }
}
