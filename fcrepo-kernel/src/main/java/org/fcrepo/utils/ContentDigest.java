package org.fcrepo.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ContentDigest {
    public final static Map<String, String> algorithmToScheme;

    static
    {
        algorithmToScheme = new HashMap<String, String>();
        algorithmToScheme.put("SHA-1", "urn:sha1");
    }

    public static URI asURI(String algorithm, String value) {
        try {
            String scheme = algorithmToScheme.get(algorithm);

            return new URI(scheme, value, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
