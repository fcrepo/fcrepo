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
package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.fcrepo.kernel.api.FedoraExternalContent.COPY;
import static org.fcrepo.kernel.api.FedoraExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * @author bseeger
 *
 * This class is a helper for dealing with the External Content Link header and External Content itself, in the case
 * of handling="copy". This class will verify that an External Content Link header is formatted correctly and
 * help parse it, delivering parts of it when asked.
 *
 */

public class ExternalContentHandler {

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    public final static String HANDLING = "handling";
    public final static String CONTENT_TYPE = "type";

    private final Link link;
    private final String handling;
    private final String type;
    private final MediaType contentType;

    /* link header for external content should look like this:
          Link: <http://example.org/some/content>;
          rel="http://fedora.info/definitions/fcrepo#ExternalContent";
          handling="proxy";
          type="image/tiff"
    */

    /**
     *  Construct an ExternalContentHandler (helper)
     *
     *  @param linkHeader actual link header from request
     */
    public ExternalContentHandler(final String linkHeader) {
        // if it parses, then we're mostly good to go.
        link = parseLinkHeader(linkHeader);

        final Map<String, String> map = link.getParams();
        // handling will be in the map, where as content type may not be
        handling = map.get(HANDLING).toLowerCase();
        type = map.get(CONTENT_TYPE) != null ? map.get(CONTENT_TYPE).toLowerCase() : null;
        contentType = type != null ? MediaType.valueOf(type) : findContentType(getURL());
    }

    /**
     * Returns the content type located in the link header.
     * @return content type if in Link header, else null
     */
    public MediaType getContentType() {
        return contentType;
    }

    /**
     * Retrieve handling information
     * @return a String containing the type of handling requested ["proxy", "copy" or "redirect"]
     */
    public String getHandling() {
        return handling;
    }

    /**
     * Retrieve url in link header
     * @return a String of the URL that was in the Link header
     */
    public String getURL() {
        return link != null ? link.getUri().toString() : null;
    }

    /**
     * Returns whether or not the handling parameter is "copy"
     * @return boolean value representing whether or not the content handling is "copy"
     */
    public boolean isCopy() {
        return handling != null ? handling.equals(COPY) : false;
    }

    /**
     * Returns whether or not the handling parameter is "redirect"
     * @return boolean value representing whether or not the content handling is "redirect"
     */
    public boolean isRedirect() {
        return handling != null ? handling.equals(REDIRECT) : false;
    }

    /**
     * Returns whether or not the handling parameter is "proxy"
     * @return boolean value representing whether or not the content handling is "proxy"
     */
    public boolean isProxy() {
        return handling != null ? handling.equals(PROXY) : false;
    }

    /**
     * Looks for ExternalContent link header and if it finds one it will return a new
     * ExternalContentHandler object based on the found Link header.
     *
     * @param links - links from the request header
     * @return External Content Handler Object if Link header found, else null
     */
    public static ExternalContentHandler createFromLinks(final List<String> links) {

        if (links == null) {
            return null;
        }

        final List<String> externalContentLinks = links.stream()
                .filter(x -> x.contains(EXTERNAL_CONTENT.toString()))
                .collect(Collectors.toList());

        if (externalContentLinks.size() > 1) {
            // got a problem, you can only have one ExternalContent links
            throw new ExternalMessageBodyException("More then one External Content Link header in request");
        } else if (externalContentLinks.size() == 1) {
            return new ExternalContentHandler(externalContentLinks.get(0));
        }

        return null;
    }

    /**
     * Fetch the external content
     * @return InputStream containing the external content
     * @throws IOException
     */
    public InputStream fetchExternalContent() throws IOException {

        final URI uri = link.getUri();
        final String scheme = uri.getScheme();
        LOGGER.info("scheme is {}", scheme);
        if (scheme != null) {
            if (scheme.equals("file")) {
                return new FileInputStream(uri.toString());
            } else if (scheme.equals("http") || scheme.equals("https")) {
                return uri.toURL().openStream();
            }
        }
        return null;
    }

    /**
     * Validate that an external content link header is appropriately formatted
     * @param link
     * @return Link object if the header is formatted correctly, else null
     * @throws ExternalMessageBodyException
     */

    private Link parseLinkHeader(final String link) throws ExternalMessageBodyException {
        final Link realLink = Link.valueOf(link);

        try {
            final String url = realLink.getUri().toString().toLowerCase();
            // see if it's a legit url, if it's not an error will be thrown
            realLink.getUri().toURL();
            if (url.isEmpty() || (!url.startsWith("http") && !url.startsWith("file"))) {
                throw new ExternalMessageBodyException("Link header formatted incorrectly: URI incorrectly formatted");
            }

            final String handling = realLink.getParams().get(HANDLING);
            if (handling == null || !handling.matches("(?i)" + PROXY + "|" + COPY + "|" + REDIRECT)) {
                // error
                throw new ExternalMessageBodyException(
                        "Link header formatted incorrectly: 'handling' parameter incorrect or missing");
            }
        } catch (final Exception e) {
            throw new ExternalMessageBodyException("External content link header url is malformed");
        }
        return realLink;
    }

    /**
     * Find the content type for a remote resource
     * @param url
     * @return the content type reported by remote system or "application/octet-stream" if not supplied
     */
    private MediaType findContentType(final String url) {
        if (url == null) {
            return null;
        }

        if (url.startsWith("file")) {
            return APPLICATION_OCTET_STREAM_TYPE;
        } else if (url.startsWith("http")) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final HttpHead httpHead = new HttpHead(url);
                try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
                    if (response.getStatusLine().getStatusCode() == SC_OK) {
                        final Header contentType = response.getFirstHeader("Content-Type");
                        if (contentType != null) {
                            return MediaType.valueOf(contentType.getValue());
                        }
                    }
                }
            } catch (final Exception e) {
                throw new RepositoryRuntimeException(e);
            }
        }
        LOGGER.debug("Defaulting to octet stream for media type");
        return APPLICATION_OCTET_STREAM_TYPE;
    }
}
