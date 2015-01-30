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
package org.modeshape.jcr.value.binary.infinispan;

import org.modeshape.jcr.value.BinaryKey;

/**
 * Utilities for gaining access to InfinispanBinaryStore internals
 * @author cabeer
 */
public class InfinispanUtils {

    private InfinispanUtils() {
    }

    /**
     * Get the data key for the given binary key
     * @param binaryStore
     * @param key
     * @return
     */
    public static String dataKeyFrom(final InfinispanBinaryStore binaryStore, final BinaryKey key) {
        return binaryStore.dataKeyFrom(key);
    }

    /**
     * Get the Metadata for the given key
     * @param binaryStore
     * @param key
     * @return
     */
    public static ChunkBinaryMetadata getMetadata(final InfinispanBinaryStore binaryStore, final BinaryKey key) {
        final String metadataKey = binaryStore.metadataKeyFrom(key);
        return new ChunkBinaryMetadata(binaryStore.metadataCache.get(metadataKey));
    }
}
