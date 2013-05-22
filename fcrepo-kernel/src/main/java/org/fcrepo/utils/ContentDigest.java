
package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

public abstract class ContentDigest {

    private static final Logger LOGGER = getLogger(ContentDigest.class);

    public static final Map<String, String> algorithmToScheme = ImmutableMap
            .of("SHA-1", "urn:sha1", "SHA1", "urn:sha1");

    public static final Map<String, String> schemeToAlgorithm = ImmutableMap
                                                                       .of("urn:sha1", "SHA-1");

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

    public static URI asURI(final String algorithm, final byte[] data) {
        return asURI(algorithm, asString(data));
    }

    public static String asString(final byte[] data) {
        return encodeHexString(data);
    }

    public static String getAlgorithm(URI digestUri) {
        return schemeToAlgorithm.get(digestUri.getScheme() + ":" + digestUri.getSchemeSpecificPart().split(":", 2)[0]);
    }
}
