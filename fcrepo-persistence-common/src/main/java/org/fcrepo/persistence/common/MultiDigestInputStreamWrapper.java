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
package org.fcrepo.persistence.common;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.fcrepo.kernel.api.utils.ContentDigest.getAlgorithm;

import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Wrapper for an InputStream that allows for the computation and evaluation
 * of multiple digests at once
 *
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapper {

    private InputStream sourceStream;

    private Map<String, String> algToDigest;

    private Map<String, DigestInputStream> algToDigestStream;

    private boolean streamRetrieved;

    /**
     * Construct a MultiDigestInputStreamWrapper
     *
     * @param sourceStream the original source input stream
     * @param digests collection of digests for the input stream
     */
    public MultiDigestInputStreamWrapper(final InputStream sourceStream, final Collection<URI> digests) {
        this.sourceStream = sourceStream;
        algToDigest = new HashMap<>();
        algToDigestStream = new HashMap<>();

        for (final URI digestUri : digests) {
            final String algorithm = getAlgorithm(digestUri);
            final String hash = substringAfterLast(digestUri.toString(), ":");
            algToDigest.put(algorithm, hash);
        }
    }

    /**
     * Get the InputStream wrapped to produce the requested digests
     *
     * @return wrapped input stream
     */
    public InputStream getInputStream() {
        streamRetrieved = true;
        InputStream digestStream = sourceStream;
        for (final String algorithm : algToDigest.keySet()) {
            try {
                // Progressively wrap the original stream in layers of digest streams
                digestStream = new DigestInputStream(
                        digestStream, MessageDigest.getInstance(algorithm));
            } catch (final NoSuchAlgorithmException e) {
                throw new RepositoryRuntimeException(e);
            }

            algToDigestStream.put(algorithm, (DigestInputStream) digestStream);
        }
        return digestStream;
    }

    /**
     * After consuming the inputstream, verify that all of the computed digests
     * matched the provided digests.
     *
     * @throws InvalidChecksumException thrown if any of the digests did not match
     */
    public void checkFixity() throws InvalidChecksumException {
        if (!streamRetrieved) {
            throw new RepositoryRuntimeException("Cannot check fixity before stream has been read");
        }
        for (final var entry: algToDigestStream.entrySet()) {
            final String algorithm = entry.getKey();
            final String originalDigest = algToDigest.get(algorithm);
            final String computed = encodeHexString(entry.getValue().getMessageDigest().digest());

            if (!originalDigest.equalsIgnoreCase(computed)) {
                throw new InvalidChecksumException(format(
                        "Checksum mismatch, computed %s digest %s did not match expected value %s",
                        algorithm, computed, originalDigest));
            }
        }
    }
}
