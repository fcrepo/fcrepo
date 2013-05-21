
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

public class CheckCacheEntryFixity implements
        Function<LowLevelCacheEntry, FixityResult> {

    private static final Logger logger =
            getLogger(LowLevelStorageService.class);

    private final MessageDigest digest;

    private final URI dsChecksum;

    private final long dsSize;

    public CheckCacheEntryFixity(final MessageDigest digest,
            final URI dsChecksum, final long dsSize) {
        this.digest = digest;
        this.dsChecksum = dsChecksum;
        this.dsSize = dsSize;
    }

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

    public MessageDigest getDigest() {
        return digest;
    }

    public URI getChecksum() {
        return dsChecksum;
    }

    public long getSize() {
        return dsSize;
    }

}
