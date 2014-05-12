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
import org.slf4j.Logger;
import com.codahale.metrics.annotation.Timed;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


/**
 * PidMinter that uses an external HTTP API to mint PIDs.
 *
 * @author escowles
 * @date 04/28/2014
 */
public class HttpPidMinter extends BasePidMinter {

    private static final Logger log = getLogger(HttpPidMinter.class);
    protected static HttpClient client = HttpClients.createSystem();
    protected String minterURL;
    protected String minterMethod;
    private String trimExpression;

    /**
     * Set the URL for the minter service.
    **/
    public void setMinterURL( final String url ) {
        minterURL = url;
    }

    /**
     * Set the HTTP method (POST, PUT or GET) used to generate a new PID.  If no method
     * is specified, POST will be used by default.
    **/
    public void setMinterMethod( final String method ) {
        minterMethod = method;
    }

    /**
     * Set the regular expression used to remove unwanted text from the minter service
     * response.  For example, if the response text is "/foo/bar:baz" and the desired
     * identifier is "baz", then the trimExpression would be ".*:".  If no expression is
     * specified, the minter service response will be used unmodified.
    **/
    public void setTrimExpression( final String expr ) {
        trimExpression = expr;
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
    public String responseToPid( final String responseText ) {
        if ( trimExpression == null ) {
            return responseText;
        } else {
            return responseText.replaceFirst(trimExpression,"");
        }
    }

    /**
     * Mint a unique identifier using an external HTTP API.
     * @return
     */
    @Timed
    @Override
    public String mintPid() {
        try {
            final HttpResponse resp = client.execute( minterRequest() );
            return responseToPid( EntityUtils.toString(resp.getEntity()) );
        } catch ( IOException ex ) {
            log.error("Error minting pid from {}: {}", minterURL, ex);
        }
        return null;
    }
}
