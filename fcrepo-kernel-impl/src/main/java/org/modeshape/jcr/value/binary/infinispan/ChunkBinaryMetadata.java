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

/**
 * Get access to the important properties in the Modeshape Binary Metadata
 * @author cabeer
 */
public class ChunkBinaryMetadata {
    private int chunkSize;
    private long length;

    /**
     * Wrap a Metadata object
     * @param metadata
     */
    public ChunkBinaryMetadata(final Metadata metadata) {
        chunkSize = metadata.getChunkSize();
        length = metadata.getLength();
    }

    /**
     * Get the content length
     * @return
     */
    public long getLength() {
        return length;
    }

    /**
     * Get the size of the binary's chunks
     * @return
     */
    public int getChunkSize() {
        return chunkSize;
    }
}
