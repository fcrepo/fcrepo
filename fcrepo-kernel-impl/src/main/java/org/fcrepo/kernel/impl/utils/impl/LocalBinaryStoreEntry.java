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

import static java.util.Objects.hash;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.impl.services.functions.GetBinaryKey;
import org.fcrepo.kernel.impl.utils.BasicCacheEntry;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;

/**
 * A LowLevelCacheEntry within a local binary store
 *
 * @author awoods
 */
public class LocalBinaryStoreEntry extends BasicCacheEntry {

    private GetBinaryKey getBinaryKey = new GetBinaryKey();
    private final BinaryStore store;
    private final Property property;

    /**
     * @param store a Modeshape BinaryStore
     * @param property the property we're interested in
     */
    public LocalBinaryStoreEntry(final BinaryStore store, final Property property) {
        this.property = property;
        this.store = store;
    }

    BinaryStore store() {
        return store;
    }

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     */
    @Override
    public InputStream getInputStream() throws RepositoryException {
        return store.getInputStream(binaryKey());
    }

    /**
     * Generate a human-readable identifier for the location of this entry
     *
     * @return human-readable identifier for the location of this entry
     */
    @Override
    public String getExternalIdentifier() {
        try {
            return new URI("info", store.toString(), null) + "/" + binaryKey();
        } catch (URISyntaxException e) {
            return binaryKey().toString();
        }
    }

    /**
     * Two LowLevelCacheEntries are the same if they have the same key,
     * come from the same BinaryStore,
     * and have the same underlying store configuration
     * @param other
     * @return true if the given binary store entries have the same key
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof LocalBinaryStoreEntry) {
            final LocalBinaryStoreEntry that = (LocalBinaryStoreEntry) other;

            return property().equals(that.property()) &&
                   ((store == null && that.store == null) ||
                    (store != null && store.equals(that.store)));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash(store, property);
    }

    protected Property property() {
        return property;
    }

    protected BinaryKey binaryKey() {
        return getBinaryKey.apply(property);
    }

}
