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

package org.fcrepo.kernel.utils;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.MISSING_STORED_FIXITY;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

abstract public class BasicCacheEntry implements CacheEntry {

    private static final Logger LOGGER = getLogger(BasicCacheEntry.class);

    /**
     * Calculate the fixity of a CacheEntry by piping it through
     * a simple fixity-calculating InputStream
     *
     * @param checksum the checksum previously generated for the entry
     * @param size the size of the entry
     * @return
     * @throws RepositoryException
     */
    @Override
    public FixityResult checkFixity(final URI checksum, final long size)
        throws RepositoryException {

        final FixityInputStream fixityInputStream;

        final String digest = ContentDigest.getAlgorithm(checksum);
        try {
            fixityInputStream = new FixityInputStream(this.getInputStream(),
                                          MessageDigest.getInstance(digest));
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Could not create MessageDigest: {}", e);
            throw propagate(e);
        }

        try {

            while (fixityInputStream.read() != -1) {
                // noop; we're just reading the stream for the checksum and size
            }

            final URI calculatedChecksum = ContentDigest.asURI(digest,
                                                                  fixityInputStream.getMessageDigest().digest());
            final FixityResult result =
                new FixityResult(this,
                                    fixityInputStream.getByteCount(),
                                    calculatedChecksum);

            if (checksum.equals(ContentDigest.missingChecksum()) || size == -1L) {
                result.status.add(MISSING_STORED_FIXITY);
            }

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
                fixityInputStream.close();
            } catch (IOException e) {
                LOGGER.debug("Got error closing input stream: {}", e);
            }
        }

    }
}
