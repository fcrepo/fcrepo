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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.ws.rs.core.UriBuilder;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;

/**
 * Convert between HTTP URIs (LDP paths) and internal Fedora ID using a
 * JAX-RS UriBuilder to mediate the URI translation.
 *
 * @author whikloj
 * @since 2019-09-26
 */
public class HttpIdentifierConverter {

    private static final Logger LOGGER = getLogger(HttpIdentifierConverter.class);

    private final UriBuilder uriBuilder;

    private final UriTemplate uriTemplate;

    private static String trimTrailingSlashes(final String string) {
        return string.replaceAll("/+$", "");
    }

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param uriBuilder the uri builder
     */
    public HttpIdentifierConverter(final UriBuilder uriBuilder) {

        this.uriBuilder = uriBuilder;
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());
    }

    /**
     * TODO: This constructor should be removed!
     * ..it is only used so that Spring can create this class... since it is unclear where the `UriBuilder` will come
     * ..from when this class is expected to be injected into `ContentExposingResource.java`
     */
    public HttpIdentifierConverter() {
        // nothing :(
        this.uriBuilder = null;
        this.uriTemplate = null;
    }

    /**
     * Convert an external URI to an internal ID.
     *
     * @param httpUri the external URI.
     * @return the internal identifier.
     */
    public String toInternalId(final String httpUri) {
        LOGGER.debug("Translating http URI {} to Fedora ID", httpUri);

        final String path = getPath(httpUri);
        if (path != null) {
            final String decodedPath = URLDecoder.decode(path, UTF_8);
            final String fedoraId = trimTrailingSlashes(decodedPath);

            return FEDORA_ID_PREFIX + fedoraId;
        }
        throw new IllegalArgumentException("Cannot translate NULL path");
    }

    /**
     * Test if the provided external URI is in the domain of this repository.
     *
     * If it is not in the domain we can't convert it.
     *
     * @param httpUri the external URI.
     * @return true if it is in domain.
     */
    public boolean inExternalDomain(final String httpUri) {
        LOGGER.debug("Checking if http URI {} is in domain", httpUri);
        return getPath(httpUri) != null;
    }

    /**
     * Convert an internal identifier to an external URI.
     *
     * @param fedoraId the internal identifier.
     * @return the external URI.
     */
    public String toExternalId(final String fedoraId) {
        LOGGER.debug("Translating Fedora ID {} to Http URI", fedoraId);
        if (inInternalDomain(fedoraId)) {
            // If it starts with our prefix, strip the prefix and any leading slashes and use it as the path
            // part of the URI.
            final String path = fedoraId.substring(FEDORA_ID_PREFIX.length()).replaceFirst("\\/", "");
            final String[] values = { path };
            // Need to pass as Array or second arg is ignored. Second arg is DON'T encode slashes
            return uriBuilder().build(values, false).toString();
        }
        throw new IllegalArgumentException("Cannot translate IDs without our prefix");
    }

    /**
     * Check if the provided internal identifier is in the domain of the repository.
     *
     * If it is not in the domain we can't convert it.
     *
     * @param fedoraId the internal identifier.
     * @return true if it is in domain.
     */
    public boolean inInternalDomain(final String fedoraId) {
        LOGGER.debug("Checking if fedora ID {} is in domain", fedoraId);
        return (fedoraId.startsWith(FEDORA_ID_PREFIX));
    }

    /**
     * Return a UriBuilder for the current template.
     *
     * @return the uri builder.
     */
    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uriBuilder.toTemplate());
    }

    /**
     * Convert a path to a full url using the UriBuilder template.
     * @param path the external path.
     * @return the full url.
     */
    public String toDomain(final String path) {

        final String realPath;
        if (path == null) {
            realPath = "";
        } else if (path.startsWith("/")) {
            realPath = path.substring(1);
        } else {
            realPath = path;
        }

        final UriBuilder uri = uriBuilder();

        if (realPath.contains("#")) {

            final String[] split = realPath.split("#", 2);

            uri.resolveTemplate("path", split[0], false);
            uri.fragment(split[1]);
        } else {
            uri.resolveTemplate("path", realPath, false);

        }
        return uri.build().toString();
    }

    /**
     * Function to convert from the external path of a URI to an internal FedoraId.
     * @param externalPath the path part of the external URI.
     * @return the FedoraId.
     */
    public FedoraId pathToInternalId(final String externalPath) {
        return FedoraId.create(toInternalId(toDomain(externalPath)));
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
