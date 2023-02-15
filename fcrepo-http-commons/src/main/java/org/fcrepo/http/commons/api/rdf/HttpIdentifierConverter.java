/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.api.rdf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

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
     * Create a new identifier converter with the given URI template.
     * @param uriBuilder the uri builder
     */
    public HttpIdentifierConverter(final UriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());
    }

    /**
     * Convert an external URI to an internal ID.
     *
     * @param httpUri the external URI.
     * @return the internal identifier.
     */
    public String toInternalId(final String httpUri) {
        return toInternalId(httpUri, false);
    }

    /**
     * Convert an external URI to an internal ID.
     *
     * @param httpUri the external URI.
     * @param encoded whether the internal ID is encoded or not.
     * @return the internal identifier.
     */
    public String toInternalId(final String httpUri, final boolean encoded) {
        LOGGER.trace("Translating http URI {} to Fedora ID with encoded set to {}", httpUri, encoded);

        final String path = getPath(httpUri);
        if (path != null) {
            final String decodedPath;
            if (!encoded) {
                decodedPath = StringUtils.uriDecode(path, UTF_8);
            } else {
                decodedPath = path;
            }
            final String fedoraId = trimTrailingSlashes(decodedPath);

            return FEDORA_ID_PREFIX + fedoraId;
        }
        throw new IllegalArgumentException("Cannot translate NULL path extracted from URI " + httpUri);
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
        LOGGER.trace("Checking if http URI {} is in domain", httpUri);
        return getPath(httpUri) != null;
    }

    /**
     * Make a URI into an absolute URI (if relative), an internal encoded ID (if in repository domain) or leave it
     * alone.
     * @param httpUri the URI
     * @return an absolute URI, the original URI or an internal ID.
     */
    public String translateUri(final String httpUri) {
        if (inExternalDomain(httpUri)) {
            return toInternalId(httpUri, true);
        } else if (httpUri.startsWith("/")) {
            // Is a relative URI.
            // Build a fake URI using the hostname so we can resolve against it.
            final var uri = uriBuilder.build("placeholder");
            return uri.resolve(httpUri).toString();
        }
        return httpUri;
    }

    /**
     * Convert an internal identifier to an external URI.
     *
     * @param fedoraId the internal identifier.
     * @return the external URI.
     */
    public String toExternalId(final String fedoraId) {
        LOGGER.trace("Translating Fedora ID {} to Http URI", fedoraId);
        if (inInternalDomain(fedoraId)) {
            // If it starts with our prefix, strip the prefix and any leading slashes and use it as the path
            // part of the URI.
            final String path = fedoraId.substring(FEDORA_ID_PREFIX.length()).replaceFirst("/", "");
            return buildUri(path);
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
        LOGGER.trace("Checking if fedora ID {} is in domain", fedoraId);
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

        return buildUri(realPath);
    }

    /**
     * Function to convert from the external path of a URI to an internal FedoraId.
     * @param externalPath the path part of the external URI.
     * @return the FedoraId.
     */
    public FedoraId pathToInternalId(final String externalPath) {
        return FedoraId.create(externalPath);
    }

    /**
     * Utility to build a URL.
     * @param path the path from the internal Id.
     * @return an external URI.
     */
    private String buildUri(final String path) {
        final UriBuilder uri = uriBuilder();
        if (path.contains("#")) {
            final String[] split = path.split("#", 2);
            uri.resolveTemplateFromEncoded("path", split[0]);
            uri.fragment(split[1]);
        } else {
            uri.resolveTemplateFromEncoded("path", path);
        }
        return uri.build().toString();
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
