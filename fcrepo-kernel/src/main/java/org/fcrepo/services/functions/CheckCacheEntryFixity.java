package org.fcrepo.services.functions;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.security.MessageDigest;

import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.slf4j.Logger;

import com.google.common.base.Function;

public class CheckCacheEntryFixity implements Function<LowLevelCacheEntry, FixityResult> {

	private static final Logger logger = getLogger(LowLevelStorageService.class);
	
	private MessageDigest digest;
	private URI dsChecksum;
	private long dsSize;
    public CheckCacheEntryFixity(final MessageDigest digest,
            final URI dsChecksum, final long dsSize) {
    	this.digest = digest;
    	this.dsChecksum = dsChecksum;
    	this.dsSize = dsSize;
    }
	@Override
	public FixityResult apply(LowLevelCacheEntry input) {
        logger.debug("Checking fixity for resource in cache store " + input.toString());
        FixityResult result = null;
        try {
        	result = input.checkFixity(dsChecksum, dsSize, digest);
        } catch (BinaryStoreException e) {
            logger.error("Exception checking low-level fixity: {}", e);
            throw new IllegalStateException(e);
		}
        return result;
	}

}
