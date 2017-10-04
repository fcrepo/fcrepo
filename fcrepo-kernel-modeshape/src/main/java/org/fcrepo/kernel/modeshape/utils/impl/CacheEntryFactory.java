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

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.modeshape.utils.BinaryCacheEntry;
import org.fcrepo.kernel.modeshape.utils.ExternalResourceCacheEntry;
import org.fcrepo.kernel.modeshape.utils.ProjectedCacheEntry;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

import com.google.common.base.Splitter;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @author lsitu
 * @author cabeer
 */
public final class CacheEntryFactory {

    public static String MESSAGE_EXTERNAL_BODY = "message/external-body";

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
        if (property.getValue().getString().indexOf((MESSAGE_EXTERNAL_BODY)) >= 0) {
            return new ExternalResourceCacheEntry(property);
        } else {
            final Binary binary = property.getBinary();

            if (binary instanceof ExternalBinaryValue) {
               return new ProjectedCacheEntry(property);
            }
            return new BinaryCacheEntry(property);
        }
    }

    /**
     * Utility method to parse the external body content in format:
     *   message/external-body; access-type=URL; url="http://www.example.com/file"
     * @param mimeType the MimeType value for external resource
     * @return Map with key mime-type for MimeType value.
     */
    public static Map<String, String> parseExternalBody(final String mimeType) {
        final Map<String, String> params = new HashMap<String, String>();
        Splitter.on(';').omitEmptyStrings().trimResults()
            .withKeyValueSeparator(Splitter.on('=').limit(2)).split("mime-type=" + mimeType.trim())
             // use lower case for keys, unwrap the quoted values (double quotes at the beginning and the end)
            .forEach((k, v) -> params.put(k.toLowerCase(), v.replaceAll("^\"|\"$", "")));
        return params;
    }
}
