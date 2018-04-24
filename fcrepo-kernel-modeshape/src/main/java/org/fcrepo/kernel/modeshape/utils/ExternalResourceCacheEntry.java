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
package org.fcrepo.kernel.modeshape.utils;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.slf4j.Logger;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Cache entry that wraps a binary stream for External Resource
 *
 * @author lsitu
 * @since 2017-09-18
 */
public class ExternalResourceCacheEntry extends BinaryCacheEntry {
    private static final Logger LOGGER = getLogger(ExternalResourceCacheEntry.class);
    /**
     * Create a new ExternalResourceCacheEntry
     * @param property the given property
     */
    public ExternalResourceCacheEntry(final Property property) {
        super(property);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.utils.CacheEntry#getInputStream()
     */
    @Override
    public InputStream getInputStream() {
        try {
            LOGGER.info("getInputStream getExternalIdentifier: {} {} ", property().getName(), getExternalIdentifier());
            return URI.create(getExternalIdentifier()).toURL().openStream();
        } catch (MalformedURLException e) {
            throw new RepositoryRuntimeException("Malformed URL: " + getExternalIdentifier(), e);
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.utils.CacheEntry#getExternalIdentifier()
     */
    @Override
    public String getExternalIdentifier() {
        try {
            LOGGER.info("getExternalIdentifier for property {} ", property().getName());
            return property().getValue().toString();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
