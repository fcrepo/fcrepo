
package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

public abstract class ContentDigest {

    private static final Logger logger = getLogger(ContentDigest.class);

    public static final Map<String, String> algorithmToScheme = ImmutableMap
            .of("SHA-1", "urn:sha1", "SHA1", "urn:sha1");

    public static URI asURI(final String algorithm, final String value) {
        try {
            final String scheme = algorithmToScheme.get(algorithm);

            return new URI(scheme, value, null);
        } catch (final URISyntaxException unlikelyException) {
            logger.warn("Exception creating checksum URI: {}",
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

    public static MessageDigest getSha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
            logger.error("Exception creating SHA-1 Digest: {}", e.getMessage());
            throw propagate(e);
        }
    }

}
