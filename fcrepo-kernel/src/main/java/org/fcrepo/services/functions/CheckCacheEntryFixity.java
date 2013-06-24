/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.ContentDigest;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.slf4j.Logger;

import com.google.common.base.Function;

/**
 * A function closing over the expected checksum information
 * for a bytestream. It transforms a particular cache entry
 * into a fixity verification.
 * @author barmintor
 * @date Apr 2, 2013
 */
public class CheckCacheEntryFixity implements
        Function<LowLevelCacheEntry, FixityResult>,
        Serializable {

    /**
     * So that it can be communicated as state to cluster members
     * during distributed fixity checks
     */
    private static final long serialVersionUID = 4701589005571818110L;

    private static final Logger logger =
            getLogger(LowLevelStorageService.class);

    private final URI dsChecksum;

    private final long dsSize;

    /**
     * A constructor providing the checksum info to close over.
     */
    public CheckCacheEntryFixity(final URI dsChecksum, final long dsSize) {
        this.dsChecksum = dsChecksum;
        this.dsSize = dsSize;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public FixityResult apply(final LowLevelCacheEntry input) {
        logger.debug("Checking fixity for resource in cache store " +
                input.toString());
        FixityResult result = null;
        try {
            result = input.checkFixity(dsChecksum, dsSize);
        } catch (final BinaryStoreException e) {
            logger.error("Exception checking low-level fixity: {}", e);
            throw propagate(e);
        }
        return result;
    }

    /**
     * Returns the digest to be used in checksum verification.
     */
    public MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance(ContentDigest.getAlgorithm(dsChecksum));
        } catch (NoSuchAlgorithmException e) {
            /**
             * @todo this really shouldn't ever happen, since it is derived from
             * an existing MessageDigest
             */
            return null;
        }
    }

    /**
     * Returns the expected checksum of the bytes to be verified.
     */
    public URI getChecksum() {
        return dsChecksum;
    }

    /**
     * Returns the expected number of bytes to be verified.
     */
    public long getSize() {
        return dsSize;
    }

}
