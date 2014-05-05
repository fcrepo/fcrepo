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
package org.fcrepo.kernel.services.functions;

import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.net.URI;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
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

    private static final Logger LOGGER =
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

    @Override
    public FixityResult apply(final LowLevelCacheEntry input) {
        LOGGER.debug("Checking fixity for resource in cache store: {} ", input);

        try {
            return input.checkFixity(dsChecksum, dsSize);
        } catch (final RepositoryException e) {
            LOGGER.error("Exception checking low-level fixity: {}", e);
            throw propagate(e);
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
