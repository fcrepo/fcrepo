/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils;

import java.io.InputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * A {@link CacheEntry} for simple Binary objects
 * @author frank asseg
 *
 */
public class BinaryCacheEntry extends BasicCacheEntry {

    private final Property property;

    /**
     * Create a new BinaryCacheEntry
     * @param property
     */
    public BinaryCacheEntry(final Property property) {
        super();
        this.property = property;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.utils.CacheEntry#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws RepositoryException {
        return property.getBinary().getStream();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.utils.CacheEntry#getExternalIdentifier()
     */
    @Override
    public String getExternalIdentifier() throws RepositoryException {
        return property.getPath();
    }

    protected Property property() {
        return property;
    }

}
