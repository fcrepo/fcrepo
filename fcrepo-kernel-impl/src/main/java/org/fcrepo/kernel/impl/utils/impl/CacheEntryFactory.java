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
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.InMemoryBinaryValue;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * @author cabeer
 */
public class CacheEntryFactory {
    private static GetBinaryStore getBinaryStore = new GetBinaryStore();

    /**
     * No public constructor on utility class
     */
    private CacheEntryFactory() {
    }

    /**
     * Load a store-specific CacheEntry model
     * @param repository
     * @param property
     * @return CacheEntry model for the property in the given repository
     * @throws RepositoryException
     */
    public static CacheEntry forProperty(final Repository repository, final Property property)
        throws RepositoryException {
        final Binary binary = property.getBinary();
        final BinaryStore store = binaryStore(repository);

        if (binary instanceof ExternalBinaryValue) {
            return new ProjectedCacheEntry(property);
        } else if (binary instanceof InMemoryBinaryValue) {
            return new BinaryCacheEntry(property);
        } else {
            return forProperty(store, property);
        }
    }

    /**
     * Get a store-specific Cache Entry
     * @param store
     * @param property
     * @return store specific cache entry
     * @throws RepositoryException
     */
    public static CacheEntry forProperty(final BinaryStore store, final Property property)
        throws RepositoryException {
        final BinaryValue binary = (BinaryValue)property.getBinary();
        if (store instanceof InfinispanBinaryStore) {
            return new InfinispanCacheStoreEntry((InfinispanBinaryStore)store, property);
        } else if (store instanceof FileSystemBinaryStore) {
            return new FileSystemBinaryStoreEntry((FileSystemBinaryStore)store, property);
        } else if (store instanceof CompositeBinaryStore) {
            final CompositeBinaryStore compositeBinaryStore = (CompositeBinaryStore) store;
            final BinaryStore binaryStoreContainingKey
                = compositeBinaryStore.findBinaryStoreContainingKey(binary.getKey());
            return forProperty(binaryStoreContainingKey, property);
        } else {
            return new LocalBinaryStoreEntry(store, property);
        }
    }

    private static BinaryStore binaryStore(final Repository repo) {
        final BinaryStore store = getBinaryStore.apply(repo);
        assert store != null;
        return store;
    }

}
