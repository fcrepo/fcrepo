/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.utils;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.FixityResult;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cache entry that wraps a binary stream and provides
 * fixity methods against it
 *
 * @author fasseg
 */
public abstract class BasicCacheEntry implements CacheEntry {

    private static final int DEV_NULL_BUFFER_SIZE = 4096;

    private static final byte[] devNull = new byte[DEV_NULL_BUFFER_SIZE];

    private static final Logger LOGGER = getLogger(BasicCacheEntry.class);

    /**
     * Calculate the fixity of a CacheEntry by piping it through
     * a simple fixity-calculating InputStream
     *
     * @param algorithm the digest algorithm to be used
     * @return the fixity of this cache entry
     */
    @Override
    public Collection<FixityResult> checkFixity(final String algorithm) {

        try (FixityInputStream fixityInputStream = new FixityInputStream(
                this.getInputStream(), MessageDigest.getInstance(algorithm))) {

            // actually calculate the digest by consuming the stream
            while (fixityInputStream.read(devNull) != -1) { }

            final URI calculatedChecksum =
                    ContentDigest.asURI(algorithm, fixityInputStream.getMessageDigest().digest());

            final FixityResult result =
                new FixityResultImpl(getExternalIdentifier(),
                                    fixityInputStream.getByteCount(),
                                    calculatedChecksum,
                                    algorithm);

            LOGGER.debug("Got {}", result.toString());

            return asList(result);
        } catch (final IOException e) {
            LOGGER.debug("Got error closing input stream: {}", e);
            throw new RepositoryRuntimeException(e);
        } catch (final NoSuchAlgorithmException e1) {
            throw new RepositoryRuntimeException(e1);
        }
    }

    /**
     * Calculate fixity with list of digest algorithms of a CacheEntry by piping it through
     * a simple fixity-calculating InputStream
     *
     * @param algorithms the digest algorithms to be used
     * @return the checksums for the digest algorithms
     * @throws UnsupportedAlgorithmException exception
     */
    @Override
    public Collection<URI> checkFixity(final Collection<String> algorithms) throws UnsupportedAlgorithmException {

        try (InputStream binaryStream = this.getInputStream()) {

            final Map<String, DigestInputStream> digestInputStreams = new HashMap<>();
            InputStream digestStream = binaryStream;
            for (String digestAlg : algorithms) {
                try {
                    digestStream = new DigestInputStream(digestStream, MessageDigest.getInstance(digestAlg));
                    digestInputStreams.put(digestAlg, (DigestInputStream)digestStream);
                } catch (NoSuchAlgorithmException e) {
                    throw new UnsupportedAlgorithmException("Unsupported digest algorithm: " + digestAlg);
                }
            }

            // calculate the digest by consuming the stream
            while (digestStream.read(devNull) != -1) { }

            return digestInputStreams.entrySet().stream()
                .map(entry -> ContentDigest.asURI(entry.getKey(), entry.getValue().getMessageDigest().digest()))
                .collect(Collectors.toSet());
        } catch (final IOException e) {
            LOGGER.debug("Got error closing input stream: {}", e);
            throw new RepositoryRuntimeException(e);
        }
    }
}
