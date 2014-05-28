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
package org.fcrepo.kernel.impl.utils.infinispan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.InternalEntryFactoryImpl;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.infinispan.metadata.EmbeddedMetadata;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A near-copy of a Modeshape class (of the same name, but is
 * unfortunately hidden from us) that takes a single OutputStream
 * and chunks it into 1MB chunks for Infinispan
 *
 * @author Chris Beer
 * @since Mar 14, 2013
 */
public class StoreChunkOutputStream extends OutputStream {

    private static final Logger LOGGER = getLogger(StoreChunkOutputStream.class);

    // 1 MB
    public static final int CHUNKSIZE = 1024 * 1024 * 1;

    private static final int CHUNK_BUFFER_SIZE = 1024;

    protected final CacheStore blobCache;

    protected final String keyPrefix;

    private final ByteArrayOutputStream chunkBuffer;

    private boolean closed;

    protected int chunkIndex;

    private final InternalEntryFactory entryFactory =
        new InternalEntryFactoryImpl();

    /**
     * Prepare to store the OutputStream in the given CacheStore
     * with the given prefix
     * @param blobCache
     * @param keyPrefix
     */
    public StoreChunkOutputStream(final CacheStore blobCache,
                                  final String keyPrefix) {
        super();
        this.blobCache = blobCache;
        this.keyPrefix = keyPrefix;
        chunkBuffer = new ByteArrayOutputStream(CHUNK_BUFFER_SIZE);
    }

    /**
     * @return Number of chunks stored.
     */
    public int getNumberChunks() {
        return chunkIndex;
    }

    @Override
    public void write(final int b) throws IOException {
        if (chunkBuffer.size() == CHUNKSIZE) {
            storeBufferInBLOBCache();
        }
        chunkBuffer.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len)
        throws IOException {
        if (len + chunkBuffer.size() <= CHUNKSIZE) {
            chunkBuffer.write(b, off, len);
        } else {
            final int storeLength = CHUNKSIZE - chunkBuffer.size();
            write(b, off, storeLength);
            storeBufferInBLOBCache();
            write(b, off + storeLength, len - storeLength);
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Close. Buffer size at close: {0}", chunkBuffer.size());
        if (closed) {
            LOGGER.debug("Stream already closed.");
            return;
        }
        closed = true;
        // store last chunk
        if (chunkBuffer.size() > 0) {
            storeBufferInBLOBCache();
        }
    }

    private void storeBufferInBLOBCache() throws IOException {
        final byte[] chunk = chunkBuffer.toByteArray();
        try {
            final String chunkKey = keyPrefix + "-" + chunkIndex;
            final InternalCacheEntry c = blobCache.load(chunkKey);
            final InternalCacheEntry cacheEntry;
            if (c == null) {
                cacheEntry =
                    entryFactory.create(chunkKey,
                                        chunk,
                                        new EmbeddedMetadata
                                        .Builder().build());
            } else {
                cacheEntry = entryFactory.create(chunkKey, chunk, c);
            }

            LOGGER.debug("Store chunk {0}", chunkKey);
            blobCache.store(cacheEntry);
            chunkIndex++;
            chunkBuffer.reset();
        } catch (final CacheLoaderException e) {
            throw new IOException(e);
        }
    }

}
