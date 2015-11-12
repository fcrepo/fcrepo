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
import java.io.FileNotFoundException;

public class BuildPropertiesLoader {

    private static final Logger LOGGER = getLogger(BuildPropertiesLoader.class);

    /**
     * This method loads build System Properties
     */

    public void loadSystemProperties() {

        final Properties buildProperties = new Properties();
        try (final InputStream pStream = getClass().getResource("/build-info.properties").openStream()) {
            buildProperties.load(pStream);
            final String projectVersion = buildProperties.getProperty("version");
            if (projectVersion != null ) {
                LOGGER.debug("project version is {}", projectVersion);
                setProperty("version", projectVersion);
            } else {
                setProperty("version", "Unknown");
            }

        } catch (final FileNotFoundException e) {
           LOGGER.info("File not Found");
           //set unknown value for all the build properties here
           setProperty("version", "Unknown");
        } catch (final IOException e) {
           LOGGER.info("IOException for property file");
           e.printStackTrace();
        }
    }

    private static void setProperty(final String prop, final String propValue) {
        System.setProperty(prop, propValue);
    }

}
