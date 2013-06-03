/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.security.MessageDigest;

import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.slf4j.Logger;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date Apr 2, 2013
 */
public class CheckCacheEntryFixity implements
        Function<LowLevelCacheEntry, FixityResult> {

    private static final Logger logger =
            getLogger(LowLevelStorageService.class);

    private final MessageDigest digest;

    private final URI dsChecksum;

    private final long dsSize;

    /**
     * @todo Add Documentation.
     */
    public CheckCacheEntryFixity(final MessageDigest digest,
            final URI dsChecksum, final long dsSize) {
        this.digest = digest;
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
            result = input.checkFixity(dsChecksum, dsSize, digest);
        } catch (final BinaryStoreException e) {
            logger.error("Exception checking low-level fixity: {}", e);
            throw propagate(e);
        }
        return result;
    }

    /**
     * @todo Add Documentation.
     */
    public MessageDigest getDigest() {
        return digest;
    }

    /**
     * @todo Add Documentation.
     */
    public URI getChecksum() {
        return dsChecksum;
    }

    /**
     * @todo Add Documentation.
     */
    public long getSize() {
        return dsSize;
    }

}
