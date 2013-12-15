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
/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.fcrepo.kernel.utils.infinispan;

import java.io.IOException;
import java.io.InputStream;

import org.infinispan.container.entries.CacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Merges chunks from cache and provides InputStream-feeling.
 *
 * @author Chris Beer
 * @date Mar 11, 2013
 */
public class StoreChunkInputStream extends InputStream {

    private static final Logger LOGGER = getLogger(StoreChunkInputStream.class);

    private final CacheStore blobCache;

    private final String key;

    protected int indexInBuffer;

    protected byte[] buffer;

    private int chunkNumber;

    /**
     * Get the chunk input stream for the given key in the given Infinispan CacheStore
     * @param blobCache
     * @param key
     */
    public StoreChunkInputStream(final CacheStore blobCache, final String key) {
        this.blobCache = blobCache;
        this.key = key;
    }

    @Override
    public int read() throws IOException {
        if (indexInBuffer == -1) {
            return -1;
        }
        if (buffer == null || indexInBuffer >= buffer.length) {
            fillBuffer();
            return read();
        }
        return buffer[indexInBuffer++] & 0xff;
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        if (indexInBuffer == -1) {
            return -1;
        }
        if (buffer == null) {
            fillBuffer();
            return read(b, off, len);
        }
        if (indexInBuffer >= buffer.length) {
            return -1;
        }
        if (indexInBuffer + len > buffer.length) {
            len = buffer.length - indexInBuffer;
        }
        System.arraycopy(buffer, indexInBuffer, b, off, len);
        indexInBuffer += len;
        // if we've just exhausted the buffer, make sure we try a new buffer on
        // next skip/read
        if (indexInBuffer == buffer.length) {
            buffer = null;
            indexInBuffer = 0;
        }
        return len;
    }

    @Override
    public int available() throws IOException {
        if (buffer == null) {
            return 0;
        }
        if (indexInBuffer >= 0) {
            return buffer.length - indexInBuffer;
        } else {
            return -1;
        }
    }

    @Override
    public final long skip(long n) throws IOException {
        if (n <= 0 || indexInBuffer == -1) {
            return 0;
        }
        if (buffer == null) {
            fillBuffer();
            return skip(n);
        }
        // do not load a new buffer if skippable bytes remain in current buffer
        if (indexInBuffer + n >= buffer.length) {
            long skipped = buffer.length - indexInBuffer;
            // but make sure a new buffer is loaded on next skip/read
            buffer = null;
            indexInBuffer = 0;
            return skipped;
        } else {
            indexInBuffer += n;
            return n;
        }
    }

    private void fillBuffer() throws IOException {

        buffer = nextChunk();
        if (buffer == null) {
            buffer = new byte[0];
            indexInBuffer = -1;
        } else {
            indexInBuffer = 0;
        }
    }

    protected byte[] nextChunk() throws IOException {
        final String chunkKey = key + "-" + chunkNumber++;
        LOGGER.debug("Read chunk {0} from cache {1}", chunkKey, blobCache);

        try {
            final CacheEntry cacheEntry = blobCache.load(chunkKey);

            if (cacheEntry == null) {
                LOGGER.trace("Unable to read chunk {0}", chunkKey);
                return null;
            }

            return (byte[]) cacheEntry.getValue();
        } catch (final CacheLoaderException e) {
            throw new IOException(e);
        }
    }
}
