/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 6, 2013
 */
public abstract class ContentDigest {

    private static final Logger LOGGER = getLogger(ContentDigest.class);

    public static final Map<String, String> algorithmToScheme = ImmutableMap
            .of("SHA-1", "urn:sha1", "SHA1", "urn:sha1");

    public static final Map<String, String> schemeToAlgorithm =
        ImmutableMap.of("urn:sha1", "SHA-1");

    /**
     * Convert a MessageDigest algorithm and checksum value to a URN
     * @param algorithm
     * @param value
     * @return
     */
    public static URI asURI(final String algorithm, final String value) {
        try {
            final String scheme = algorithmToScheme.get(algorithm);

            return new URI(scheme, value, null);
        } catch (final URISyntaxException unlikelyException) {
            LOGGER.warn("Exception creating checksum URI: {}",
                               unlikelyException);
            throw propagate(unlikelyException);
        }
    }

    /**
     * Convert a MessageDigest algorithm and checksum byte-array data to a URN
     * @param algorithm
     * @param data
     * @return
     */
    public static URI asURI(final String algorithm, final byte[] data) {
        return asURI(algorithm, asString(data));
    }

    /**
     * Given a digest URI, get the corresponding MessageDigest algorithm
     * @param digestUri
     * @return
     */
    public static String getAlgorithm(URI digestUri) {
        return schemeToAlgorithm
            .get(digestUri.getScheme() + ":" +
                 digestUri.getSchemeSpecificPart().split(":", 2)[0]);
    }

    private static String asString(final byte[] data) {
        return encodeHexString(data);
    }

}
