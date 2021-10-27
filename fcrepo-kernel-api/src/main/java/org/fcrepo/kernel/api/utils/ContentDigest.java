/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import org.fcrepo.config.DigestAlgorithm;
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
            final String scheme = DigestAlgorithm.getScheme(algorithm);

            return new URI(scheme, value, null);
        } catch (final URISyntaxException unlikelyException) {
            LOGGER.warn("Exception creating checksum URI: alg={}; value={}",
                               algorithm, value);
            throw new RepositoryRuntimeException(unlikelyException.getMessage(), unlikelyException);
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
        return DigestAlgorithm.fromScheme(digestUri.getScheme() + ":" +
             digestUri.getSchemeSpecificPart().split(":", 2)[0]).getAlgorithm();
    }

    private static String asString(final byte[] data) {
        return encodeHexString(data);
    }

}
