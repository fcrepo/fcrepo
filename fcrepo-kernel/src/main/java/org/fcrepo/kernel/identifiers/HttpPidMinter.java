/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.lang.StringUtils.isBlank;
import static com.google.common.base.Preconditions.checkNotNull;

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


/**
 * PidMinter that uses an external HTTP API to mint PIDs.
 *
 * @author escowles
 * @date 04/28/2014
 */
public class HttpPidMinter extends BasePidMinter {

    private static final Logger log = getLogger(HttpPidMinter.class);
    protected HttpClient client;
    protected String minterURL;
    protected String minterMethod;
    protected String username;
    protected String password;
    private String trimExpression;
    private XPathExpression xpathExpression;

    /**
     * Set the URL for the minter service.
    **/
    public void setMinterURL( final String url ) {
        log.debug("setMinterURL({})",url);
        this.minterURL = url;
    }

    /**
     * Set the HTTP method (POST, PUT or GET) used to generate a new PID.  If no method
     * is specified, POST will be used by default.
    **/
    public void setMinterMethod( final String method ) {
        log.debug("setMinterMethod({})",method);
        this.minterMethod = method;
    }

    /**
     * Set the regular expression used to remove unwanted text from the minter service
     * response.  For example, if the response text is "/foo/bar:baz" and the desired
     * identifier is "baz", then the trimExpression would be ".*:".
    **/
    public void setTrimExpression( final String expr ) {
        log.debug("setTrimExpression({})",expr);
        this.trimExpression = expr;
    }

    /**
     * Set the XPath expression used to extract the desired identifier from an XML minter
     * response.
    **/
    public void setXPathExpression( final String xpath ) {
        log.debug("setXPathExpression({})",xpath);
        if ( xpath != null ) {
            try {
                this.xpathExpression = XPathFactory.newInstance().newXPath().compile(xpath);
            } catch ( XPathException ex ) {
                log.warn("Error parsing xpath ({}): {}", xpath, ex );
            }
        }
    }

    /**
     * Set the username used to connect to the minter service.  Unless both username and password
     * are set, no authentication will be used.
    **/
    public void setUsername( final String username ) {
        log.debug("setUsername({})",username);
        this.username = username;
    }

    /**
     * Set the password used to connect to the minter service.  Unless both username and password
     * are set, no authentication will be used.
    **/
    public void setPassword( final String password ) {
        log.debug("setPassword({})",password);
        this.password = password;
    }

    /**
     * Setup authentication in httpclient.
    **/
    private HttpClient buildClient() {
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties().setConnectionManager(
            new PoolingHttpClientConnectionManager());
        if (!isBlank(username) && !isBlank(password)) {
            final URI uri = URI.create(minterURL);
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, password));
            builder = builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder.build();
    }

    /**
     * Instantiate a request object based on the minterMethod variable.
    **/
    protected HttpUriRequest minterRequest() {
        if ( minterMethod != null && minterMethod.equalsIgnoreCase("GET") ) {
            return new HttpGet(minterURL);
        } else if ( minterMethod != null && minterMethod.equalsIgnoreCase("PUT") ) {
            return new HttpPut(minterURL);
        } else {
            return new HttpPost(minterURL);
        }
    }

    /**
     * Remove unwanted text from the minter service response to produce the desired identifer.
     * Override this method for processing more complex than a simple regex replacement.
    **/
    public String responseToPid( final String responseText ) throws Exception {
        log.debug("responseToPid({})", responseText);
        if ( trimExpression != null ) {
            return responseText.replaceFirst(trimExpression,"");
        } else if ( xpathExpression != null ) {
            return xpath( responseText, xpathExpression );
        } else {
            return responseText;
        }
    }

    /**
     * Extract the desired identifier value from an XML response using XPath
    **/
    private static String xpath( String xml, XPathExpression xpath ) throws Exception {
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
        checkNotNull( minterURL, "Minter URL must be specified!" );
        try {
            log.debug("mintPid()");
            if ( client == null ) {
                client = buildClient();
            }
            final HttpResponse resp = client.execute( minterRequest() );
            return responseToPid( EntityUtils.toString(resp.getEntity()) );
        } catch ( IOException ex ) {
            log.warn("Error minting pid from {}: {}", minterURL, ex);
        } catch ( Exception ex ) {
            log.warn("Error processing minter response", ex);
        }
        return null;
    }
}
