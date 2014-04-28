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
package org.fcrepo.integration.auth.oauth.api;

import static java.util.regex.Pattern.compile;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

public class TokenEndpointIT extends AbstractOAuthResourceIT {

    private final String tokenRegexp = "access_token\":\"(.+?)\"";

    private final Pattern tokenPattern = compile(tokenRegexp);

    @Test
    public void testGetToken() throws Exception {
        logger.trace("Entering testGetToken()...");
        final HttpPost post =
                new HttpPost(
                        tokenEndpoint +
                                "?grant_type=password&username=foo&password=bar&client_secret=foo&client_id=bar");
        post.addHeader("Accept", APPLICATION_JSON);
        post.addHeader("Content-type", APPLICATION_FORM_URLENCODED);
        final HttpResponse tokenResponse = client.execute(post);
        logger.debug("Got a token response: \n{}", EntityUtils
                .toString(tokenResponse.getEntity()));
        Assert.assertEquals("Couldn't retrieve a token from token endpoint!",
                            200,
                            tokenResponse.getStatusLine().getStatusCode());

    }

    @Test
    public void testUseToken() throws ClientProtocolException, IOException {
        final String pid = UUID.randomUUID().toString();
        logger.trace("Entering testUseToken()...");
        logger.debug("Trying to write an object to authenticated area without authentication via token...");
        final HttpResponse failure =
                client.execute(putObjMethod("authenticated/" + pid));
        Assert.assertEquals(
                "Was able to write to an authenticated area when I shouldn't be able to",
                SC_UNAUTHORIZED,
                failure.getStatusLine().getStatusCode());
        logger.debug("Failed as expected.");
        logger.debug("Now trying with authentication via token...");
        final HttpPost post =
                new HttpPost(
                        tokenEndpoint +
                                "?grant_type=password&username=foo&password=bar&client_secret=foo&client_id=bar");
        post.addHeader("Accept", APPLICATION_JSON);
        post.addHeader("Content-type", APPLICATION_FORM_URLENCODED);
        final HttpResponse tokenResponse = client.execute(post);
        final String tokenResponseString =
                EntityUtils.toString(tokenResponse.getEntity());
        logger.debug("Got a token response: \n{}", tokenResponseString);
        Assert.assertEquals("Couldn't retrieve a token from token endpoint!",
                            200,
                            tokenResponse.getStatusLine().getStatusCode());

        final Matcher tokenMatcher = tokenPattern.matcher(tokenResponseString);
        Assert.assertTrue("Couldn't find token in token response!", tokenMatcher
                .find());
        final String token = tokenMatcher.group(1);
        logger.debug("Found token: {}", token);
        final HttpPut successMethod =
                putObjMethod("authenticated/" + pid + "?access_token=" +
                                 token);
        final HttpResponse success = client.execute(successMethod);
        Assert.assertEquals("Failed to create object even with authentication!",
                            201,
                            success.getStatusLine().getStatusCode());
    }
}
