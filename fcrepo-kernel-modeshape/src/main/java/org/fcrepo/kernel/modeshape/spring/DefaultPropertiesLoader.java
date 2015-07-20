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
package org.fcrepo.kernel.modeshape.spring;

import org.slf4j.Logger;

import java.io.File;

import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class loads System Properties only if:
 * - the context is not an integration-test, and
 * - the property is not already set
 * This class mutates explicitly specified system
 * Properties only if:
 * - they represent relative paths in order to
 *   make them relative to the explicit or implicit
 *   home directory
 *
 * @author Andrew Woods
 *         Date: 10/15/13
 */
public class DefaultPropertiesLoader {

    private static final Logger LOGGER = getLogger(DefaultPropertiesLoader.class);

    private static final String SEP = getProperty("file.separator");

    /**
     * @author awoods
     * @since 2013
     */
    private enum PROPERTIES {
        DEFAULT_OBJECT_STORE(
                "com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean.default.objectStoreDir"),
        OBJECT_STORE("com.arjuna.ats.arjuna.objectstore.objectStoreDir"),
        ISPN_CACHE("fcrepo.ispn.cache"),
        ISPN_BIN_CACHE("fcrepo.ispn.binary.cache"),
        BIN_STORE_PATH("fcrepo.binary.directory"),
        MODE_INDEX("fcrepo.modeshape.index.directory"),
        ISPN_ALT_CACHE("fcrepo.ispn.alternative.cache"),
        ISPN_BIN_ALT_CACHE("fcrepo.ispn.binary.alternative.cache"),
        ISPN_REPO_CACHE("fcrepo.ispn.repo.cache"),
        ACTIVE_MQ("fcrepo.activemq.directory");

        private String text;

        private PROPERTIES(final String text) {
            this.text = text;
        }

        public String getValue() {
            return text;
        }
    }


    /**
     * This method loads default System Properties if:
     * - the context is not an integration-test, and
     * - the property is not already set
     * This method mutates explicitly specified system
     * Properties only if:
     * - they represent relative paths in order to
     *   make them relative to the explicit or implicit
     *   home directory
     */
    public void loadSystemProperties() {
        LOGGER.info("Loading properties");

        if (getProperty("integration-test") == null) {
            LOGGER.trace("Setting default properties, if necessary.");
            final String fcrepoHome = getProperty("fcrepo.home");
            final String baseDir = (fcrepoHome == null
                    ? getProperty("user.dir") + SEP + "fcrepo4-data" + SEP
                    : fcrepoHome + SEP);
            for (final PROPERTIES prop : PROPERTIES.values()) {
                final String value = getProperty(prop.getValue());
                if (value == null) {
                    setProperty(prop.getValue(), baseDir);
                } else {
                    updateRelativePropertyPath(prop.getValue(), value, baseDir);
                }
            }
        }

        for (final PROPERTIES prop : PROPERTIES.values()) {
            final String val = prop.getValue();
            LOGGER.info("{} = {}", val, getProperty(val));
        }
    }

    private static void setProperty(final String prop, final String baseDir) {
        System.setProperty(prop, baseDir + prop);
    }

    private static void updateRelativePropertyPath(final String prop, final String value, final String baseDir) {
        if (!new File(value).isAbsolute()) {
            System.setProperty(prop, baseDir + value);
        }
    }

}
