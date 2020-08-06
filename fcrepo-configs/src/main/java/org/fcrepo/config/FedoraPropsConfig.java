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
 * General Fedora properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
public class FedoraPropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraPropsConfig.class);

    public static final String FCREPO_HOME = "fcrepo.home";

    private static final String DATA_DIR = "data";

    @Value("${" + FCREPO_HOME + ":fcrepo-home}")
    private Path fedoraHome;

    @Value("#{fedoraPropsConfig.fedoraHome.resolve('" + DATA_DIR + "')}")
    private Path fedoraData;

    @PostConstruct
    private void postConstruct() throws IOException {
        LOGGER.info("Fedora home: {}", fedoraHome);
        LOGGER.debug("Fedora home data: {}", fedoraData);
        try {
            Files.createDirectories(fedoraHome);
        } catch (IOException e) {
            throw new IOException(String.format("Failed to create Fedora home directory at %s." +
                    " Fedora home can be configured by setting the %s property.", fedoraHome, FCREPO_HOME), e);
        }
        Files.createDirectories(fedoraData);
    }

    /**
     * @return Path to Fedora home directory
     */
    public Path getFedoraHome() {
        return fedoraHome;
    }

    /**
     * Sets the path to the Fedora home directory -- should only be used for testing purposes.
     *
     * @param fedoraHome Path to Fedora home directory
     */
    public void setFedoraHome(final Path fedoraHome) {
        this.fedoraHome = fedoraHome;
    }

    /**
     * @return Path to Fedora home data directory
     */
    public Path getFedoraData() {
        return fedoraData;
    }

    /**
     * Sets the path to the Fedora home data directory -- should only be used for testing purposes.
     *
     * @param fedoraData Path to Fedora home data directory
     */
    public void setFedoraData(final Path fedoraData) {
        this.fedoraData = fedoraData;
    }

}
