/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.kernel.modeshape.rdf.impl;

/**
 *
 * @author Nianli Ma
 *         Date: 11/11/15
 */

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class BuildPropertiesLoader {

    private static final Logger LOGGER = getLogger(BuildPropertiesLoader.class);

    // The following keys come from: fcrepo-webapp/src/main/resources/build-info.properties
    private static final String BUILD_NAME_KEY = "build.name";
    private static final String BUILD_DATE_KEY = "build.date";
    private static final String BUILD_REVISION_KEY = "build.revision";

    private String buildName;
    private String buildDate;
    private String buildRevision;

    /**
     * This method loads build Properties
     */

    public BuildPropertiesLoader() {

        final Properties buildProperties = new Properties();
        try (final InputStream pStream = getClass().getResourceAsStream("/build-info.properties")) {
            buildProperties.load(pStream);

        } catch (final IOException e) {
           LOGGER.info("IOException for build-info.properties file");
        //   e.printStackTrace();
        buildName = buildProperties.getProperty(BUILD_NAME_KEY, "unknown");
        buildDate = buildProperties.getProperty(BUILD_DATE_KEY, "unknown");
        buildRevision = buildProperties.getProperty(BUILD_REVISION_KEY, "unknown");
        }

        buildName = buildProperties.getProperty(BUILD_NAME_KEY, "unknown");
        buildDate = buildProperties.getProperty(BUILD_DATE_KEY, "unknown");
        buildRevision = buildProperties.getProperty(BUILD_REVISION_KEY, "unknown");
    }

    /**
     * Get build name
     * @return build name
     */
    public String getBuildName() {
        return buildName;
    }

    /**
     * Get build date
     * @return build date
     */
    public String getBuildDate() {
        return buildDate;
    }

    /**
     * Get build revision
     * @return build revision
     */
    public String getBuildRevision() {
        return buildRevision;
    }
}
