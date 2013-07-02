/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.auth.oauth.integration.api;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class AuthzEndpointIT extends AbstractOAuthResourceIT {

    @Test
    public void testGetAuthzCode() throws ClientProtocolException, IOException {
        // we want to grab the auth code, not get redirected
        client.getParams().setBooleanParameter(HANDLE_REDIRECTS, false);
        logger.debug("Entering testGetAuthzCode()...");
        final HttpResponse response =
                client.execute(new HttpGet(authzEndpoint +
                        "?client_id=CLIENT-ID&redirect_uri=http://example.com&response_type=code"));
        logger.debug("Retrieved authorization endpoint response.");
        final String redirectHeader =
                response.getFirstHeader("Location").getValue();
        final String authCode =
                URI.create(redirectHeader).getQuery().split("&")[0].split("=")[1];
        assertNotNull("Didn't find authorization code!", authCode);
        logger.debug("with authorization code: {}", authCode);

    }

    @Test
    public void testUseAuthCode() throws ClientProtocolException, IOException {
        // we want to grab the auth code, not get redirected
        client.getParams().setBooleanParameter(HANDLE_REDIRECTS, false);
        logger.debug("Entering testUseAuthCode()...");
        final HttpResponse response =
                client.execute(new HttpGet(authzEndpoint +
                        "?client_id=CLIENT-ID&redirect_uri=http://example.com&response_type=code"));
        logger.debug("Retrieved authorization endpoint response.");
        final String redirectHeader =
                response.getFirstHeader("Location").getValue();
        final String authCode =
                URI.create(redirectHeader).getQuery().split("&")[0].split("=")[1];
        assertNotNull("Didn't find authorization code!", authCode);
        logger.debug("with authorization code: {}", authCode);
        final HttpPost post =
                new HttpPost(tokenEndpoint +
                        "?grant_type=authorization_code&code=" + authCode +
                        "&client_secret=foo&client_id=CLIENT-ID&redirect_uri=http://example.com");
        post.addHeader("Accept", APPLICATION_JSON);
        post.addHeader("Content-type", APPLICATION_FORM_URLENCODED);
        final HttpResponse tokenResponse = client.execute(post);
        logger.debug("Got a token response: \n{}", EntityUtils
                .toString(tokenResponse.getEntity()));
        assertEquals("Couldn't retrieve a token from token endpoint!", 200,
                tokenResponse.getStatusLine().getStatusCode());
    }

}
