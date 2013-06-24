package org.fcrepo.utils.impl;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.slf4j.Logger;


public class LocalBinaryStoreEntry extends LowLevelCacheEntry {

    private static final Logger LOGGER = getLogger(LocalBinaryStoreEntry.class);

    private final BinaryStore store;

    public LocalBinaryStoreEntry(final BinaryStore store, final BinaryKey key) {
        super(key);
        this.store = store;
    }

    /**
     * @todo Add Documentation.
     */
    BinaryStore getStore() {
        return store;
    }

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     * @throws BinaryStoreException
     */
    public InputStream getInputStream() throws BinaryStoreException {
        return store.getInputStream(key);
    }

    /**
     * Send a raw input stream to the underlying store for this entry; used for
     * fixing e.g. fixity failures.
     *
     * @param stream binary content to REPLACE the content in the store
     * @throws BinaryStoreException
     * @throws IOException
     */
    public void storeValue(final InputStream stream)
        throws BinaryStoreException, IOException {
        // TODO: this is probably an auditable action.
        LOGGER.info("Doing a low-level write to store {} for key {}",
                    getExternalIdentifier(), key);

        store.storeValue(stream);
    }

    /**
     * Generate a human-readable identifier for the location of this entry
     *
     * @return
     */
    public String getExternalIdentifier() {

        // TODO : I wonder if this could/should be a JSON blob or something
        //  machine parsable as well?

        if ( store instanceof FileSystemBinaryStore) {
            final FileSystemBinaryStore fsStore = (FileSystemBinaryStore)store;
            return getExternalId() + "/" + store.getClass().getName() + ":" +
                fsStore.getDirectory().toPath();
        } else {
            return getExternalId() + "/" + store.toString();
        }
    }
    
    /**
     * Two LowLevelCacheEntries are the same if they have the same key,
     * come from the same BinaryStore,
     * and have the same underlying store configuration
     * @param other
     * @return
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof LocalBinaryStoreEntry) {
            final LocalBinaryStoreEntry that = (LocalBinaryStoreEntry) other;

            return key.equals(that.getKey()) &&
                   ((store == null && that.store == null) ||
                    (store != null && store.equals(that.store)));
        } else {
            return false;
        }
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public int hashCode() {
        return hash(store, key);
    }

}
