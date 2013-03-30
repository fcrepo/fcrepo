
package org.fcrepo.utils;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class ContentDigest {

    public static final Map<String, String> algorithmToScheme = ImmutableMap
            .of("SHA-1", "urn:sha1","SHA1", "urn:sha1");

    public static URI asURI(String algorithm, String value) {
        try {
            String scheme = algorithmToScheme.get(algorithm);

            return new URI(scheme, value, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static URI asURI(String algorithm, byte[] data) {
    	return asURI(algorithm, asString(data));
    }
    
    public static String asString(byte[] data) {
    	return encodeHexString(data);
    }
}
