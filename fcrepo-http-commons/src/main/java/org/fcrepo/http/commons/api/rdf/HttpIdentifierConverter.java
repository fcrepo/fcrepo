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
package org.fcrepo.http.commons.api.rdf;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Converter;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;

/**
 * Convert between HTTP URIs (LDP paths) and internal Fedora ID using a
 * JAX-RS UriBuilder to mediate the URI translation.
 *
 * @author whikloj
 * @since 2019-09-26
 */
public class HttpIdentifierConverter extends Converter<String, String> {

    private static final Logger LOGGER = getLogger(HttpIdentifierConverter.class);

    private final UriBuilder uriBuilder;

    protected Converter<String, String> forward = identity();
    protected Converter<String, String> reverse = identity();

    private final UriTemplate uriTemplate;

    private static final String FEDORA_ID_PREFIX = "info:fedora/";

    /**
     * Split uri on delim and return the first part.
     */
    private static BiFunction<String, String, String> removeDelim =
        (uri, delim) -> {
            final String newUri;
            if (uri.indexOf(delim) >= 0) {
                newUri = uri.split(delim)[0];
            } else {
                newUri = uri;
            }
            return newUri;
        };

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param uriBuilder the uri builder
     */
    public HttpIdentifierConverter(final UriBuilder uriBuilder) {

        this.uriBuilder = uriBuilder;
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uriBuilder.toTemplate());
    }

    @Override
    protected String doForward(final String httpUri) {
        LOGGER.debug(String.format("Translating http URI %s to Fedora ID", httpUri));

        final String path = getPath(httpUri);
        if (path != null) {

            // Take the URL and remove any hash uris, or fcr: endpoints.
            final String fedoraId = Arrays.asList(path).stream().map(p -> removeDelim.apply(p, "#"))
                .map(p -> removeDelim.apply(p, "/" + FCR_METADATA))
                .map(p -> removeDelim.apply(p, "/" + FCR_ACL))
                .map(p -> removeDelim.apply(p, "/" + FCR_VERSIONS)).findFirst().orElse("");

            return FEDORA_ID_PREFIX + fedoraId.replaceFirst("\\/", "");
        }
        return "";

    }

    @Override
    protected String doBackward(final String fedoraId) {
        LOGGER.debug(String.format("Translating Fedora ID %s to Http URI", fedoraId));
        if (fedoraId.startsWith(FEDORA_ID_PREFIX)) {
            // If it starts with our prefix, strip the prefix and use it as the path
            // part of the URI.
            final String[] values = { fedoraId.substring(FEDORA_ID_PREFIX.length()) };
            // Need to pass as Array or second arg is ignored. Second arg is DON'T encode slashes
            return uriBuilder().build(values, false).toString();
        }
        return "";
    }

    /**
     * Split the path off the URI.
     *
     * @param httpUri the incoming URI.
     * @return the path of the URI.
     */
    private String getPath(final String httpUri) {
        final Map<String, String> values = new HashMap<>();

        if (uriTemplate.match(httpUri, values) && values.containsKey("path")) {
            return "/" + values.get("path");
        } else if (isRootWithoutTrailingSlash(httpUri)) {
            return "/";
        }
        return null;
    }

    /**
     * Test if the URI is the root but missing the trailing slash
     *
     * @param httpUri the incoming URI.
     * @return whether or not it is the root minus trailing slash
     */
    private boolean isRootWithoutTrailingSlash(final String httpUri) {
        final Map<String, String> values = new HashMap<>();

        return uriTemplate.match(httpUri + "/", values) && values.containsKey("path") &&
            values.get("path").isEmpty();
    }

}
