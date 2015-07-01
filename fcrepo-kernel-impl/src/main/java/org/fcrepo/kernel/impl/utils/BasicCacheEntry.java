/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.utils;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cache entry that wraps a binary stream and provides
 * fixity methods against it
 *
 * @author fasseg
 */
public abstract class BasicCacheEntry implements CacheEntry {

    public static final int DEV_NULL_BUFFER_SIZE = 4096;

    private static final byte[] devNull = new byte[DEV_NULL_BUFFER_SIZE];

    private static final Logger LOGGER = getLogger(BasicCacheEntry.class);

    /**
     * Calculate the fixity of a CacheEntry by piping it through
     * a simple fixity-calculating InputStream
     *
     * @param digest the digest
     * @return the fixity of this cache entry
     * @throws RepositoryException if repository exception occurred
     */
    @Override
    public Collection<FixityResult> checkFixity(final String digest)
        throws RepositoryException {

        try (FixityInputStream fixityInputStream = new FixityInputStream(this.getInputStream(),
                MessageDigest.getInstance(digest))) {
            // exhaust our source
            while (fixityInputStream.read(devNull) != -1) { }

            final URI calculatedChecksum = ContentDigest.asURI(digest,
                                                                  fixityInputStream.getMessageDigest().digest());
            final FixityResult result =
                new FixityResultImpl(this,
                                    fixityInputStream.getByteCount(),
                                    calculatedChecksum);

            LOGGER.debug("Got {}", result.toString());

            return asList(result);
        } catch (final IOException e) {
            LOGGER.debug("Got error closing input stream: {}", e);
            throw new RepositoryRuntimeException(e);
        } catch (final NoSuchAlgorithmException e1) {
            throw new RepositoryRuntimeException(e1);
        }

    }
}
