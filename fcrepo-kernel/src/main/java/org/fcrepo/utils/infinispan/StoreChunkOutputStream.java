
package org.fcrepo.utils.infinispan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.InternalEntryFactoryImpl;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.modeshape.common.logging.Logger;

public class StoreChunkOutputStream extends OutputStream {

    protected final Logger logger;

    public static final int CHUNKSIZE = 1024 * 1024 * 1; // 1 MB

    protected final CacheStore blobCache;

    protected final String keyPrefix;

    private final ByteArrayOutputStream chunkBuffer;

    private boolean closed;

    protected int chunkIndex;

    private final InternalEntryFactory entryFactory =
            new InternalEntryFactoryImpl();

    public StoreChunkOutputStream(CacheStore blobCache, String keyPrefix) {
        logger = Logger.getLogger(getClass());
        this.blobCache = blobCache;
        this.keyPrefix = keyPrefix;
        chunkBuffer = new ByteArrayOutputStream(1024);
    }

    /**
     * @return Number of chunks stored.
     */
    public int getNumberChunks() {
        return chunkIndex;
    }

    @Override
    public void write(int b) throws IOException {
        if (chunkBuffer.size() == CHUNKSIZE) {
            storeBufferInBLOBCache();
        }
        chunkBuffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len + chunkBuffer.size() <= CHUNKSIZE) {
            chunkBuffer.write(b, off, len);
        } else {
            int storeLength = CHUNKSIZE - chunkBuffer.size();
            write(b, off, storeLength);
            storeBufferInBLOBCache();
            write(b, off + storeLength, len - storeLength);
        }
    }

    @Override
    public void close() throws IOException {
        logger.debug("Close. Buffer size at close: {0}", chunkBuffer.size());
        if (closed) {
            logger.debug("Stream already closed.");
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
            String chunkKey = keyPrefix + "-" + chunkIndex;
            InternalCacheEntry c = blobCache.load(chunkKey);
            final InternalCacheEntry cacheEntry;
            if (c == null) {
                cacheEntry =
                        entryFactory.create(chunkKey, chunk,
                                (EntryVersion) null);
            } else {
                cacheEntry = entryFactory.create(chunkKey, chunk, c);
            }

            logger.debug("Store chunk {0}", chunkKey);
            blobCache.store(cacheEntry);
        } catch (CacheLoaderException e) {
            throw new IOException(e);
        }

    }

}
