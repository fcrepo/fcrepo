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
package org.fcrepo.kernel.modeshape.utils.impl;

import static org.fcrepo.kernel.api.RdfLexicon.PROXY_FOR;
import static org.fcrepo.kernel.api.RdfLexicon.REDIRECTS_TO;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.modeshape.utils.BinaryCacheEntry;
import org.fcrepo.kernel.modeshape.utils.ExternalResourceCacheEntry;
import org.fcrepo.kernel.modeshape.utils.ProjectedCacheEntry;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @author lsitu
 * @author cabeer
 */
public final class CacheEntryFactory {

    private static final Logger LOGGER = getLogger(CacheEntryFactory.class);

    /**
     * No public constructor on utility class
     */
    private CacheEntryFactory() {
    }

    /**
     * Load a store-specific CacheEntry model
     * @param property the property
     * @return CacheEntry model for the property in the given repository
     * @throws RepositoryException if repository exception occurred
     */
    public static CacheEntry forProperty(final Property property) throws RepositoryException {
        // if it's an external binary, catch that here and treat it differently.
        LOGGER.info("Did you know that property.getName() is: '{}' and Proxy: {} ", property.getName(),
                PROXY_FOR.getNameSpace() + ":" + PROXY_FOR.getLocalName());
        if (property.getName().endsWith(PROXY_FOR.getLocalName()) ||
                property.getName().endsWith(REDIRECTS_TO.getLocalName())) {
            LOGGER.info("Creating ExternalResourceCacheEntry for property: {} {}", property.getName(),
                    property.getValue().toString());
            return new ExternalResourceCacheEntry(property);
        }

        final Binary binary = property.getBinary();

        if (binary instanceof ExternalBinaryValue) {
           return new ProjectedCacheEntry(property);
        }
        return new BinaryCacheEntry(property);
    }
}
