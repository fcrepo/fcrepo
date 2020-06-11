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

package org.fcrepo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fedora's OCFL related configuration properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
public class OcflPropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPropsConfig.class);

    public static final String FCREPO_OCFL_STAGING = "fcrepo.ocfl.staging";
    public static final String FCREPO_OCFL_ROOT = "fcrepo.ocfl.root";
    public static final String FCREPO_OCFL_TEMP = "fcrepo.ocfl.temp";

    private static final String OCFL_STAGING = "staging";
    private static final String OCFL_ROOT = "ocfl-root";
    private static final String OCFL_TEMP = "ocfl-temp";

    @Value("${" + FCREPO_OCFL_STAGING + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_STAGING + "')}}")
    private Path fedoraOcflStaging;

    @Value("${" + FCREPO_OCFL_ROOT + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_ROOT + "')}}")
    private Path ocflRepoRoot;

    @Value("${" + FCREPO_OCFL_TEMP + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_TEMP + "')}}")
    private Path ocflTemp;

    @PostConstruct
    private void postConstruct() throws IOException {
        LOGGER.info("Fedora staging: {}", fedoraOcflStaging);
        LOGGER.info("Fedora OCFL root: {}", ocflRepoRoot);
        LOGGER.info("Fedora OCFL temp: {}", ocflTemp);
        Files.createDirectories(fedoraOcflStaging);
        Files.createDirectories(ocflRepoRoot);
        Files.createDirectories(ocflTemp);
    }

    /**
     * @return Path to directory Fedora stages resources before moving them into OCFL
     */
    public Path getFedoraOcflStaging() {
        return fedoraOcflStaging;
    }

    /**
     * Sets the path to the Fedora staging directory -- should only be used for testing purposes.
     *
     * @param fedoraOcflStaging Path to Fedora staging directory
     */
    public void setFedoraOcflStaging(final Path fedoraOcflStaging) {
        this.fedoraOcflStaging = fedoraOcflStaging;
    }

    /**
     * @return Path to OCFL root directory
     */
    public Path getOcflRepoRoot() {
        return ocflRepoRoot;
    }

    /**
     * Sets the path to the Fedora OCFL root directory -- should only be used for testing purposes.
     *
     * @param ocflRepoRoot Path to Fedora OCFL root directory
     */
    public void setOcflRepoRoot(final Path ocflRepoRoot) {
        this.ocflRepoRoot = ocflRepoRoot;
    }

    /**
     * @return Path to the temp directory used by the OCFL client
     */
    public Path getOcflTemp() {
        return ocflTemp;
    }

    /**
     * Sets the path to the OCFL temp directory -- should only be used for testing purposes.
     *
     * @param ocflTemp Path to OCFL temp directory
     */
    public void setOcflTemp(final Path ocflTemp) {
        this.ocflTemp = ocflTemp;
    }

}
