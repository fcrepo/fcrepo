package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;


/**
 * Created by bseeger on 4/18/18.
 */

public class ExternalContentHandler {

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    public final static String PROXY = "proxy";
    public final static String HANDLING = "handling";
    public final static String COPY = "copy";
    public final static String REDIRECT = "redirect";
    public final static String CONTENT_TYPE = "type";

    private final String originalLinkHeader;
    private final String url;
    private final String handling;
    private final String type;
    private final MediaType contentType;

    /* link should look like this:
          Link: <http://example.org/some/content>;
          rel="http://fedora.info/definitions/fcrepo#ExternalContent";
          handling="proxy";
          type="image/tiff"
    */

    public ExternalContentHandler(final String linkHeader) {
        final Link externalLink = parseLinkHeader(linkHeader);

        // if it gets past that, good, parse it
        verifyRequestForExternalBody(externalLink);
        originalLinkHeader = linkHeader;
        final Map<String, String> map = externalLink.getParams();
        url = map.get("url");
        handling = map.get("HANDLING");
        type = map.get(CONTENT_TYPE);
        contentType = type != null ? MediaType.valueOf(type) : findContentType(url);
    }

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
                        LOGGER.debug("findContentType got {} status", response.getStatusLine().getStatusCode());
                        final String contentType = response.getFirstHeader("Content-Type").getValue();
                        if (contentType != null) {
                            LOGGER.debug("findContentType got {} mediaType", contentType);
                            return MediaType.valueOf(contentType);
                        }
                    }

                }
            } catch (final Exception e) {
                throw new ExternalMessageBodyException("Failed to get remote content type for external message");
            }
        }
        LOGGER.debug("Defaulting to octet stream for media type");
        return APPLICATION_OCTET_STREAM_TYPE;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public String getHandling() {
        return handling;
    }

    public String getURL() throws MalformedURLException {
        return url;
    }

    public Boolean isCopy() {
        return handling == COPY ? true : false;
    }

    public Boolean isRedirect() {
        return handling == REDIRECT ? true : false;
    }

    public Boolean isProxy() {
        return handling == PROXY ? true : false;
    }

    /**
     * Looks for ExternalContent link header
     *
     * @param links - links from the request header
     * @return External Content link header if found, else null
     */
    static String findExternalLink(final List<String> links) {

        if (links == null) {
            return null;
        }

        final List<String> externalContentLinks = links.stream()
                .filter(x -> x.contains(EXTERNAL_CONTENT.toString()))
                .collect(Collectors.toList());

        if (externalContentLinks.size() > 1) {
            // got a problem, you can only have one ExternalContent links
            // todo - throw error with constrained by info
        } else if (externalContentLinks.size() == 1) {
            return externalContentLinks.get(0);
        }

        return null;
    }

    public InputStream fetchExternalContent() throws IOException {

        if (handling == COPY) {
            if (url.startsWith("file://")) {
                return new FileInputStream(url);
            } else if (url.startsWith("http")) {
                return new URL(url).openStream();
            }
        }
        return null;
    }

    /**
     * Examines a link header for ExternalContent to verify that it's legit
     * @param parsedHeader
     * @throws ExternalMessageBodyException
     */
    private void verifyRequestForExternalBody(final Link externalLink) throws ExternalMessageBodyException {

        try {
            final URL url = externalLink.getUri().toURL();
        } catch (final MalformedURLException e) {
            throw new ExternalMessageBodyException("External content link header url is malformed");
        }

        // if no 'handling' key, error with constrainedBy
        // if 'handling' key, but it's not {copy, proxy, redirect} throw error with constrainedBy
        final String handling = externalLink.getParams().get(HANDLING);
        if (handling == null || !handling.matches("(?i)" + PROXY + "|" + COPY + "|" + REDIRECT)) {
            // error
            throw new ExternalMessageBodyException(
                    "Link header formatted incorrectly: 'handling' parameter incorrect");
        }

        // if we get this far, things are good
    }

    private Link parseLinkHeader(final String link) throws ExternalMessageBodyException {
        final Link realLink = Link.valueOf(link);

        final String url = realLink.getUri().toString().toLowerCase();
        if (url.isEmpty() || !url.startsWith("http") || !url.startsWith("file")) {
            throw new ExternalMessageBodyException("Link header formatted incorrectly: URI incorrectly formatted");
        }
        return realLink;
    }

}
