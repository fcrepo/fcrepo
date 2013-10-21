/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.spring;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class loads System Properties only if:
 * - the context is not an integration-test, and
 * - the property is not already set
 *
 * @author Andrew Woods
 *         Date: 10/15/13
 */
public class DefaultPropertiesLoader {

    private static final Logger LOGGER = getLogger(DefaultPropertiesLoader.class);

    final private static String SEP = getProperty("file.separator");
    final private static String BASEDIR = getProperty("java.io.tmpdir") + SEP
            + "fcrepo4-data" + SEP;

    /**
     * @author awoods
     * @date 2013
     */
    private enum PROPERTIES {
        DEFAULT_OBJECT_STORE(
                "com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean.default.objectStoreDir"),
        OBJECT_STORE("com.arjuna.ats.arjuna.objectstore.objectStoreDir"),
        ISPN_CACHE("fcrepo.ispn.CacheDirPath"),
        ISPN_BIN_CACHE("fcrepo.ispn.binary.CacheDirPath"),
        MODE_INDEX("fcrepo.modeshape.index.location"),
        ISPN_ALT_CACHE("fcrepo.ispn.alternative.CacheDirPath"),
        ISPN_BIN_ALT_CACHE("fcrepo.ispn.binary.alternative.CacheDirPath"),
        ISPN_REPO_CACHE("fcrepo.ispn.repo.CacheDirPath"),
        ACTIVE_MQ("fcrepo.activemq.dir");

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
     */
    public void loadSystemProperties() {
        LOGGER.info("Loading properties");

        if (getProperty("integration-test") == null) {
            LOGGER.debug("Setting default properties, if necessary.");

            for (final PROPERTIES prop : PROPERTIES.values()) {
                if (getProperty(prop.getValue()) == null) {
                    setProperty(prop.getValue());
                }
            }
        }

        for (final PROPERTIES prop : PROPERTIES.values()) {
            final String val = prop.getValue();
            LOGGER.debug("{} = {}", val, getProperty(val));
        }
    }

    private static String getProperty(final String property) {
        return System.getProperty(property);
    }

    private void setProperty(final String prop) {
        System.setProperty(prop, BASEDIR + prop);
    }

}
