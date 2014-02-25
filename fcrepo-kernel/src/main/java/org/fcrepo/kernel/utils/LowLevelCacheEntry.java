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
package org.fcrepo.kernel.utils;

import java.io.IOException;
import java.io.InputStream;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * Manage low-level I/O from a cache store
 * (or, for an ISPN store, a cache loader) in order
 * to report on e.g. fixity.
 *
 * @author Chris Beer
 * @date Mar 15, 2013
 */
public abstract class LowLevelCacheEntry extends BasicCacheEntry {

    protected static final String DATA_SUFFIX = "-data";


    protected String externalId;

    protected final BinaryKey key;

    protected LowLevelCacheEntry(final BinaryKey key) {
        super();
        this.key = key;
        this.externalId = "";
    }

    /**
     * Send a raw input stream to the underlying store for this entry; used for
     * fixing e.g. fixity failures.
     *
     * @param stream binary content to REPLACE the content in the store
     * @throws BinaryStoreException
     * @throws IOException
     */
    public abstract void storeValue(final InputStream stream)
            throws BinaryStoreException, IOException;

    /**
     * Set a meaningful identifier from some higher level that we should
     * dutifully pass through.
     *
     * @param externalId some identifier for the cache store
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Return the external identifier.
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Get the BinaryKey for this cache entry
     * @return
     */
    public BinaryKey getKey() {
        return key;
    }

}
