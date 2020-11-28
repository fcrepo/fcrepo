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
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fcrepo.kernel.api.exception.ExternalContentAccessException;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import javax.ws.rs.core.Link;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.fcrepo.kernel.api.models.ExternalContent;
import org.slf4j.Logger;

/**
 * This class is a helper for dealing with the External Content Link header and External Content itself, in the case
 * of handling="copy". This class will verify that an External Content Link header is formatted correctly and
 * help parse it, delivering parts of it when asked.
 *
 * @author bseeger
 * @since 5/7/2018
 */
public class ExternalContentHandler implements ExternalContent {

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    private final static String HANDLING = "handling";
    private final static String EXT_CONTENT_TYPE = "type";

    private final Link link;
    private final String handling;
    private String contentType;
    private Long contentSize;

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
    protected ExternalContentHandler(final String linkHeader) {
        // if it parses, then we're mostly good to go.
        link = parseLinkHeader(linkHeader);

        final Map<String, String> map = link.getParams();
        // handling will be in the map, where as content type may not be
        handling = map.get(HANDLING).toLowerCase();
        // Retrieve details directly from the content
        retrieveContentDetails();
        final var type = map.get(EXT_CONTENT_TYPE) != null ? map.get(EXT_CONTENT_TYPE).toLowerCase() : null;
        if (type != null) {
            contentType = type;
        } else if (contentType == null) {
            LOGGER.debug("Defaulting to octet stream for media type");
            contentType = APPLICATION_OCTET_STREAM_TYPE.toString();
        }

        if (contentSize == null) {
            contentSize = -1l;
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public long getContentSize() {
        return contentSize;
    }

    @Override
    public String getHandling() {
        return handling;
    }

    @Override
    public String getURL() {
        return link.getUri().toString();
    }

    @Override
    public URI getURI() {
        return link.getUri();
    }

    @Override
    public boolean isCopy() {
        return COPY.equals(handling);
    }

    @Override
    public boolean isRedirect() {
        return REDIRECT.equals(handling);
    }

    @Override
    public boolean isProxy() {
        return PROXY.equals(handling);
    }

    @Override
    public InputStream fetchExternalContent() {

        final URI uri = link.getUri();
        final String scheme = uri.getScheme();
        LOGGER.debug("scheme is {}", scheme);
        if (scheme != null) {
            try {
                if (scheme.equals("file")) {
                    return new FileInputStream(uri.getPath());
                } else if (scheme.equals("http") || scheme.equals("https")) {
                    return uri.toURL().openStream();
                }
            } catch (final IOException e) {
                throw new ExternalContentAccessException("Failed to read external content from " + uri, e);
            }
        }
        return null;
    }

    /**
     * Validate that an external content link header is appropriately formatted
     * @param link to be validated
     * @return Link object if the header is formatted correctly, else null
     * @throws ExternalMessageBodyException on error
     */
    private Link parseLinkHeader(final String link) throws ExternalMessageBodyException {
        final Link realLink;

        try {
            realLink = Link.valueOf(link);
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

    private void retrieveContentDetails() {
        final URI uri = getURI();
        final String scheme = uri.getScheme().toLowerCase();

        if ("file".equals(scheme)) {
            final Path path = Paths.get(uri);
            try {
                contentSize = Files.size(path);
            } catch (final IOException e) {
                throw new ExternalMessageBodyException("Unable to access external binary at URI " + uri, e);
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final HttpHead httpHead = new HttpHead(uri);
                httpHead.setHeader("Accept-Encoding", "identity");
                try (final CloseableHttpResponse response = httpClient.execute(httpHead)) {
                    if (response.getStatusLine().getStatusCode() != SC_OK) {
                        throw new ExternalMessageBodyException("Unable to access external binary at URI " + uri
                                + " received response " + response.getStatusLine().getStatusCode());
                    }

                    final Header typeHeader = response.getFirstHeader(CONTENT_TYPE);
                    if (typeHeader != null) {
                        contentType = typeHeader.getValue();
                    }
                    final Header sizeHeader = response.getFirstHeader(CONTENT_LENGTH);
                    if (sizeHeader != null) {
                        contentSize = Long.parseLong(sizeHeader.getValue());
                    }
                }
            } catch (final IOException e) {
                throw new ExternalMessageBodyException("Unable to access external binary at URI " + uri, e);
            }
        }
    }
}
