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
package org.fcrepo.kernel.api.utils;

import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.slf4j.Logger;

/**
 * Digest helpers to convert digests (checksums) into URI strings
 * (based loosely on Magnet URIs)
 * @author Chris Beer
 * @since Mar 6, 2013
 */
public final class ContentDigest {

    private static final Logger LOGGER = getLogger(ContentDigest.class);

    public enum DIGEST_ALGORITHM {
        SHA1("SHA", "urn:sha1"), SHA256("SHA-256", "urn:sha256"), MD5("MD5", "urn:md5"), MISSING("NONE", "missing");

        final public String algorithm;
        final private String scheme;

        DIGEST_ALGORITHM(final String alg, final String scheme) {
            this.algorithm = alg;
            this.scheme = scheme;
        }

        /**
         * Return the scheme associated with the provided algorithm (e.g. SHA-1 returns urn:sha1)
         *
         * @param alg for which scheme is requested
         * @return scheme
         */
        public static String getScheme(final String alg) {
            return Arrays.stream(values()).filter(value ->
                    value.algorithm.equalsIgnoreCase(alg) || value.algorithm.replace("-", "").equalsIgnoreCase(alg)
            ).findFirst().orElse(MISSING).scheme;
        }

        /**
         * Return enum value for the provided scheme (e.g. urn:sha1 returns SHA-1)
         *
         * @param argScheme for which enum is requested
         * @return enum value associated with the arg scheme
         */
        public static DIGEST_ALGORITHM fromScheme(final String argScheme) {
            return Arrays.stream(values()).filter(value -> value.scheme.equalsIgnoreCase(argScheme)
            ).findFirst().orElse(MISSING);
        }

        /**
         * Return enum value for the provided algorithm (e.g. SHA-1)
         *
         * @param argAlgorithm for which enum is requested
         * @return enum value associated with the arg algorithm
         */
        public static DIGEST_ALGORITHM fromAlgorithm(final String argAlgorithm) {
            return Arrays.stream(values()).filter(value -> value.algorithm.equalsIgnoreCase(argAlgorithm)
            ).findFirst().orElse(MISSING);
        }

        /**
         * Return true if the provided algorithm is included in this enum
         *
         * @param alg to test
         * @return true if arg algorithm is supported
         */
        public static boolean isSupportedAlgorithm(final String alg) {
            return !getScheme(alg).equals(MISSING.scheme);
        }
    }

    public static final String DEFAULT_ALGORITHM = DIGEST_ALGORITHM.SHA1.algorithm;

    private ContentDigest() {
    }

    /**
     * Convert a MessageDigest algorithm and checksum value to a URN
     * @param algorithm the message digest algorithm
     * @param value the checksum value
     * @return URI
     */
    public static URI asURI(final String algorithm, final String value) {
        try {
            final String scheme = DIGEST_ALGORITHM.getScheme(algorithm);

            return new URI(scheme, value, null);
        } catch (final URISyntaxException unlikelyException) {
            LOGGER.warn("Exception creating checksum URI: {}",
                               unlikelyException);
            throw new RepositoryRuntimeException(unlikelyException);
        }
    }

    /**
     * Convert a MessageDigest algorithm and checksum byte-array data to a URN
     * @param algorithm the message digest algorithm
     * @param data the checksum byte-array data
     * @return URI
     */
    public static URI asURI(final String algorithm, final byte[] data) {
        return asURI(algorithm, asString(data));
    }

    /**
     * Given a digest URI, get the corresponding MessageDigest algorithm
     * @param digestUri the digest uri
     * @return MessageDigest algorithm
     */
    public static String getAlgorithm(final URI digestUri) {
        if (digestUri == null) {
            return DEFAULT_ALGORITHM;
        }
        return DIGEST_ALGORITHM.fromScheme(digestUri.getScheme() + ":" +
             digestUri.getSchemeSpecificPart().split(":", 2)[0]).algorithm;
    }

    private static String asString(final byte[] data) {
        return encodeHexString(data);
    }

    /**
     * Placeholder checksum value.
     * @return URI
     */
    public static URI missingChecksum() {
        return asURI(SHA1.algorithm, SHA1.scheme);
    }

}
