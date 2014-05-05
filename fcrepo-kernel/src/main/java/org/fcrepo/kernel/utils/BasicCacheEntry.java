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
package org.fcrepo.kernel.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.MISSING_STORED_FIXITY;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cache entry that wraps a binary stream and provides
 * fixity methods against it
 *
 * @author fasseg
 */
public abstract class BasicCacheEntry implements CacheEntry {

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

        final String digest = ContentDigest.getAlgorithm(checksum);

        try (FixityInputStream fixityInputStream = new FixityInputStream(this.getInputStream(),
                MessageDigest.getInstance(digest))) {

            IOUtils.copy(fixityInputStream, NULL_OUTPUT_STREAM);

            final URI calculatedChecksum = ContentDigest.asURI(digest,
                                                                  fixityInputStream.getMessageDigest().digest());
            final FixityResult result =
                new FixityResultImpl(this,
                                    fixityInputStream.getByteCount(),
                                    calculatedChecksum);

            if (checksum == null || checksum.equals(ContentDigest.missingChecksum()) || size == -1L) {
                result.getStatus().add(MISSING_STORED_FIXITY);
            }

            if (!result.matches(checksum)) {
                result.getStatus().add(BAD_CHECKSUM);
            }

            if (!result.matches(size)) {
                result.getStatus().add(BAD_SIZE);
            }

            if (result.matches(size, checksum)) {
                result.getStatus().add(SUCCESS);
            }

            LOGGER.debug("Got {}", result.toString());

            return result;
        } catch (final IOException e) {
            LOGGER.debug("Got error closing input stream: {}", e);
            throw propagate(e);
        } catch (final NoSuchAlgorithmException e1) {
            throw propagate(e1);
        }

    }
}
