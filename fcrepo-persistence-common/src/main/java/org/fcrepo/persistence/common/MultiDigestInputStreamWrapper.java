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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;

/**
 * Wrapper for an InputStream that allows for the computation and evaluation
 * of multiple digests at once
 *
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapper {

    private final InputStream sourceStream;

    private final Map<String, String> algToDigest;

    private final Map<String, DigestInputStream> algToDigestStream;

    private boolean streamRetrieved;

    private Map<String, String> computedDigests;

    /**
     * Construct a MultiDigestInputStreamWrapper
     *
     * @param sourceStream the original source input stream
     * @param digests collection of digests for the input stream
     * @param wantDigests list of additional digest algorithms to compute for the input stream
     */
    public MultiDigestInputStreamWrapper(final InputStream sourceStream, final Collection<URI> digests,
            final Collection<DIGEST_ALGORITHM> wantDigests) {
        this.sourceStream = sourceStream;
        algToDigest = new HashMap<>();
        algToDigestStream = new HashMap<>();

        if (digests != null) {
            for (final URI digestUri : digests) {
                final String algorithm = getAlgorithm(digestUri);
                final String hash = substringAfterLast(digestUri.toString(), ":");
                algToDigest.put(algorithm, hash);
            }
        }

        // Merge the list of wanted digest algorithms with set of provided digests
        if (wantDigests != null) {
            for (final DIGEST_ALGORITHM wantDigest : wantDigests) {
                if (!algToDigest.containsKey(wantDigest.algorithm)) {
                    algToDigest.put(wantDigest.algorithm, null);
                }
            }
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
                throw new UnsupportedAlgorithmException("Unsupported digest algorithm: " + algorithm, e);
            }

            algToDigestStream.put(algorithm, (DigestInputStream) digestStream);
        }
        return digestStream;
    }

    /**
     * After consuming the inputstream, verify that all of the computed digests
     * matched the provided digests.
     *
     * Note: the wrapped InputStream will be consumed if it has not already been read.
     *
     * @throws InvalidChecksumException thrown if any of the digests did not match
     */
    public void checkFixity() throws InvalidChecksumException {
        calculateDigests();

        algToDigest.forEach((algorithm, originalDigest) -> {
            // Skip any algorithms which were calculated but no digest was provided for verification
            if (originalDigest == null) {
                return;
            }
            final String computed = computedDigests.get(algorithm);

            if (!originalDigest.equalsIgnoreCase(computed)) {
                throw new InvalidChecksumException(format(
                        "Checksum mismatch, computed %s digest %s did not match expected value %s",
                        algorithm, computed, originalDigest));
            }
        });

    }

    /**
     * Returns the list of digests calculated for the wrapped InputStream
     *
     * Note: the wrapped InputStream will be consumed if it has not already been read.
     *
     * @return list of digests calculated from the wrapped InputStream, in URN format.
     */
    public List<URI> getDigests() {
        calculateDigests();

        return computedDigests.entrySet().stream()
                .map(e -> ContentDigest.asURI(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Get the digest calculated for the provided algorithm
     *
     * @param alg algorithm of the digest to retrieve
     * @return the calculated digest, or null if no digest of that type was calculated
     */
    public String getDigest(final DIGEST_ALGORITHM alg) {
        calculateDigests();

        return computedDigests.entrySet().stream()
                .filter(entry -> alg.algorithm.equals(entry.getKey()))
                .map(Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private void calculateDigests() {
        if (computedDigests != null) {
            return;
        }

        if (!streamRetrieved) {
            // Stream not previously consumed, consume it now in order to calculate digests
            try (final InputStream is = getInputStream()) {
                while (is.read() != -1) {
                }
            } catch (final IOException e) {
                throw new RepositoryRuntimeException("Failed to read content stream while calculating digests", e);
            }
        }

        computedDigests = new HashMap<>();
        algToDigestStream.forEach((algorithm, digestStream) -> {
            final String computed = encodeHexString(digestStream.getMessageDigest().digest());
            computedDigests.put(algorithm, computed);
        });
    }
}
