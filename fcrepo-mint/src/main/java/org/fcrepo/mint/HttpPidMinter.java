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
import static org.apache.commons.lang.StringUtils.isBlank;
import static com.google.common.base.Preconditions.checkArgument;

import org.slf4j.Logger;

import com.codahale.metrics.annotation.Timed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
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
import org.fcrepo.kernel.identifiers.PidMinter;


/**
 * PID minter that uses an external REST service to mint PIDs.
 *
 * @author escowles
 * @since 04/28/2014
 */
public class HttpPidMinter implements PidMinter {

    private static final Logger log = getLogger(HttpPidMinter.class);
    protected final String url;
    protected final String method;
    protected final String username;
    protected final String password;
    private final String regex;
    private XPathExpression xpath;

    protected HttpClient client;

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

        checkArgument( !isBlank(url), "Minter URL must be specified!" );

        this.url = url;
        this.method = (method == null ? "post" : method);
        this.username = username;
        this.password = password;
        this.regex = regex;
        if ( !isBlank(xpath) ) {
            try {
                this.xpath = XPathFactory.newInstance().newXPath().compile(xpath);
            } catch ( XPathException ex ) {
                log.warn("Error parsing xpath ({}): {}", xpath, ex );
                throw new IllegalArgumentException("Error parsing xpath" + xpath, ex);
            }
        }
        this.client = buildClient();
    }

    /**
     * Setup authentication in httpclient.
    **/
    protected HttpClient buildClient() {
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties().setConnectionManager(
            new PoolingHttpClientConnectionManager());
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
        switch (method) {
            case "GET": case "get":
                return new HttpGet(url);
            case "PUT": case "put":
                return new HttpPut(url);
            default:
                return new HttpPost(url);
        }
    }

    /**
     * Remove unwanted text from the minter service response to produce the desired identifer.
     * Override this method for processing more complex than a simple regex replacement.
    **/
    protected String responseToPid( final String responseText ) throws Exception {
        log.debug("responseToPid({})", responseText);
        if ( !isBlank(regex) ) {
            return responseText.replaceFirst(regex,"");
        } else if ( xpath != null ) {
            return xpath( responseText, xpath );
        } else {
            return responseText;
        }
    }

    /**
     * Extract the desired identifier value from an XML response using XPath
    **/
    private static String xpath( final String xml, final XPathExpression xpath ) throws Exception {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return xpath.evaluate(doc);
    }

    /**
     * Mint a unique identifier using an external HTTP API.
     * @return The generated identifier.
     */
    @Timed
    @Override
    public String mintPid() {
        try {
            log.debug("mintPid()");
            final HttpResponse resp = client.execute( minterRequest() );
            return responseToPid( EntityUtils.toString(resp.getEntity()) );
        } catch ( IOException ex ) {
            log.warn("Error minting pid from {}: {}", url, ex);
            throw new RuntimeException("Error minting pid", ex);
        } catch ( Exception ex ) {
            log.warn("Error processing minter response", ex);
            throw new RuntimeException("Error processing minter response", ex);
        }
    }
}
