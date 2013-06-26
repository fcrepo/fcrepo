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

package org.fcrepo.integration.webhooks;

import static java.lang.Thread.sleep;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.integration.webhooks.TestEndpoint.lastBody;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration({"/spring-test/test-container.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FedoraWebhooksIT extends AbstractResourceIT {

    @Test
    public void registerWebhookCallbackTest() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "/fcr:webhooks/callback_id");

        final List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl",
                "info:fedora/fake:url"));
        final UrlEncodedFormEntity entity =
                new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));

        final HttpGet displayWebhooks =
                new HttpGet(serverAddress + "/fcr:webhooks");

        final String content =
                EntityUtils.toString(client.execute(displayWebhooks)
                        .getEntity());

        logger.info("Got content: ");
        logger.info(content);
        assertTrue("Our webhook wasn't registered!", compile(
                "info:fedora/fake:url", DOTALL).matcher(content).find());

    }

    public void deleteWebhookTest() throws Exception {
        final HttpPost method =
                new HttpPost(serverAddress + "/fcr:webhooks/callback_id");

        final List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl",
                "info:fedora/fake:url"));
        final UrlEncodedFormEntity entity =
                new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));

        final HttpDelete delete_method =
                new HttpDelete(serverAddress + "/fcr:webhooks/callback_id");

        assertEquals(204, getStatus(delete_method));
    }

    @Test
    public void FireWebhooksTest() throws IOException {

        final HttpPost method =
                new HttpPost(serverAddress + "/fcr:webhooks/callback_id");

        final List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl", serverAddress +
                "/dummy"));
        final UrlEncodedFormEntity entity =
                new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));

        final HttpPost create_method =
                new HttpPost(serverAddress + "/rest/fcr:new");
        assertEquals(201, getStatus(create_method));

        try {
            for (int i = 0; i < 200; i++) {

                sleep(200);

                if (lastBody != null) {
                    break;
                }
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        logger.debug(lastBody);

        assertNotNull("Our webhook wasn't called!", lastBody);
        assertTrue("Our webhook didn't have the content we expected!", compile(
                "ingest", DOTALL).matcher(lastBody).find());

    }
}
