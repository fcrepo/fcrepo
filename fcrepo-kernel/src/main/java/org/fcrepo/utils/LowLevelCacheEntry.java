/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static java.util.Objects.hash;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.utils.FixityResult.FixityState.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.poi.util.IOUtils;
import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.fcrepo.utils.infinispan.StoreChunkOutputStream;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

/**
 * Manage low-level I/O from a cache store
 * (or, for an ISPN store, a cache loader) in order
 * to report on e.g. fixity.
 *
 * @author Chris Beer
 * @date Mar 15, 2013
 */
public abstract class LowLevelCacheEntry {

    private static final Logger LOGGER = getLogger(LowLevelCacheEntry.class);

    protected static final String DATA_SUFFIX = "-data";


    protected String externalId;

    protected final BinaryKey key;
    
    protected LowLevelCacheEntry(final BinaryKey key) {
    	this.key = key;
        this.externalId = "";
    }

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     * @throws BinaryStoreException
     */
    public abstract InputStream getInputStream() throws BinaryStoreException;

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
     * Generate a human-readable identifier for the location of this entry
     *
     * @return
     */
    public abstract String getExternalIdentifier();

    /**
     * Check the entry's InputStream against the checksum and size.
     *
     * @param checksum
     * @param size
     * @param digest
     * @return
     * @throws BinaryStoreException
     */
    public FixityResult checkFixity(final URI checksum, final long size)
        throws BinaryStoreException {
        final FixityInputStream ds;
        final String digest = ContentDigest.getAlgorithm(checksum);
        try {
            ds = new FixityInputStream(getInputStream(),
                                       MessageDigest.getInstance(digest));
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Could not create MessageDigest: {}", e);
            throw propagate(e);
        }

        try {

            while (ds.read() != -1) {
                // noop; we're just reading the stream for the checksum and size
            }

            final FixityResult result =
                new FixityResult(this, ds.getByteCount(),
                                 ContentDigest
                                 .asURI(digest,
                                        ds.getMessageDigest().digest()));

            if (!result.matches(checksum)) {
                result.status.add(BAD_CHECKSUM);
            }

            if (!result.matches(size)) {
                result.status.add(BAD_SIZE);
            }

            if (result.matches(size, checksum)) {
                result.status.add(SUCCESS);
            }

            LOGGER.debug("Got {}", result.toString());

            return result;
        } catch (final IOException e) {
            throw propagate(e);
        } finally {
            try {
                ds.close();
            } catch (IOException e) {
                LOGGER.debug("Got error closing input stream: {}", e);
            }
        }

    }

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
     * @todo Add Documentation.
     */
    public BinaryKey getKey() {
        return key;
    }

}
