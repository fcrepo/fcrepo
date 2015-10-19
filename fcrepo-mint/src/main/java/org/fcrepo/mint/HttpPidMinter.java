/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.mint;

import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.slf4j.Logger;

import com.codahale.metrics.annotation.Timed;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
import org.xml.sax.SAXException;


/**
 * PID minter that uses an external REST service to mint PIDs.
 *
 * @author escowles
 * @since 04/28/2014
 */
public class HttpPidMinter implements UniqueValueSupplier {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final Logger LOGGER = getLogger(HttpPidMinter.class);
    protected final String url;
    protected final String method;
    protected final String username;
    protected final String password;
    private final String regex;
    private XPathExpression xpath;

    protected HttpClient client;
    private final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    /**
     * Create a new HttpPidMinter.
     * @param url The URL for the minter service.  This is the only required argument -- all
     *    other parameters can be blank.
     * @param method The HTTP method (POST, PUT or GET) used to generate a new PID (POST will
     *    be used if the method is blank.
     * @param username If not blank, use this username to connect to the minter service.
     * @param password If not blank, use this password used to connect to the minter service.
     * @param regex If not blank, use this regular expression used to remove unwanted text from the
     *    minter service response.  For example, if the response text is "/foo/bar:baz" and the
     *    desired identifier is "baz", then the regex would be ".*:".
     * @param xpath If not blank, use this XPath expression used to extract the desired identifier
     *    from an XML minter response.
    **/
    public HttpPidMinter( final String url, final String method, final String username,
        final String password, final String regex, final String xpath ) {

        if (isBlank(url)) {
            throw new IllegalArgumentException("Minter URL must be specified!");
        }

        this.url = url;
        this.method = (method == null ? "post" : method);
        this.username = username;
        this.password = password;
        this.regex = regex;
        if ( !isBlank(xpath) ) {
            try {
                this.xpath = XPathFactory.newInstance().newXPath().compile(xpath);
            } catch ( final XPathException ex ) {
                LOGGER.warn("Error parsing xpath ({}): {}", xpath, ex.getMessage());
                throw new IllegalArgumentException("Error parsing xpath" + xpath, ex);
            }
        }
        this.client = buildClient();
    }

    /**
     * Setup authentication in httpclient.
     * @return the setup of authentication
    **/
    protected HttpClient buildClient() {
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties().setConnectionManager(connManager);
        if (!isBlank(username) && !isBlank(password)) {
            final URI uri = URI.create(url);
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, password));
            builder = builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder.build();
    }

    /**
     * Instantiate a request object based on the method variable.
    **/
    private HttpUriRequest minterRequest() {
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "PUT":
                return new HttpPut(url);
            default:
                return new HttpPost(url);
        }
    }

    /**
     * Remove unwanted text from the minter service response to produce the desired identifier.
     * Override this method for processing more complex than a simple regex replacement.
     * @param responseText the response text
     * @throws IOException if exception occurred
     * @return the response
    **/
    protected String responseToPid( final String responseText ) throws IOException {
        LOGGER.debug("responseToPid({})", responseText);
        if ( !isBlank(regex) ) {
            return responseText.replaceFirst(regex,"");
        } else if ( xpath != null ) {
            try {
                return xpath( responseText, xpath );
            } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
                throw new IOException(e);
            }
        } else {
            return responseText;
        }
    }

    /**
     * Extract the desired identifier value from an XML response using XPath
    **/
    private static String xpath( final String xml, final XPathExpression xpath )
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        final DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return xpath.evaluate(doc);
    }

    /**
     * Mint a unique identifier using an external HTTP API.
     * @return The generated identifier.
     */
    @Timed
    @Override
    public String get() {
        try {
            LOGGER.debug("mintPid()");
            final HttpResponse resp = client.execute( minterRequest() );
            return responseToPid( EntityUtils.toString(resp.getEntity()) );
        } catch ( final IOException ex ) {
            LOGGER.warn("Error minting pid from {}: {}", url, ex.getMessage());
            throw new PidMintingException("Error minting pid", ex);
        } catch ( final Exception ex ) {
            LOGGER.warn("Error processing minter response", ex.getMessage());
            throw new PidMintingException("Error processing minter response", ex);
        }
    }
}
