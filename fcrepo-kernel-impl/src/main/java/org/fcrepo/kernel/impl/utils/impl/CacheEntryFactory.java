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
package org.fcrepo.kernel.impl.utils.impl;

import org.fcrepo.kernel.impl.utils.BinaryCacheEntry;
import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.impl.utils.ProjectedCacheEntry;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @author cabeer
 */
public final class CacheEntryFactory {

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
        final Binary binary = property.getBinary();

        if (binary instanceof ExternalBinaryValue) {
            return new ProjectedCacheEntry(property);
        } else {
            return new BinaryCacheEntry(property);
        }
    }
}
