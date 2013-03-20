package org.fcrepo.integration.webhooks;


import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
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
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/rest.xml",
        "/spring-test/eventing.xml"})
public class FedoraWebhooksIT extends AbstractResourceIT {

    @Test
    public void registerWebhookCallbackTest() throws IOException {
        HttpPost method = new HttpPost(serverAddress +
                "/webhooks/callback_id");

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl", "info:fedora/fake:url"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));


        final HttpGet displayWebhooks =
                new HttpGet(serverAddress + "/webhooks");

        String content = EntityUtils.toString(client.execute(
                displayWebhooks).getEntity());

        logger.info("Got content: ");
        logger.info(content);
        assertTrue("Our webhook wasn't registered!", compile(
                "info:fedora/fake:url", DOTALL).matcher(content)
                .find());


    }

    public void deleteWebhookTest() throws Exception {
        HttpPost method = new HttpPost(serverAddress +
                "/webhooks/callback_id");

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl", "info:fedora/fake:url"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));


        HttpDelete delete_method = new HttpDelete(serverAddress + "/webhooks/callback_id");


        assertEquals(204, getStatus(delete_method));
    }

    @Test
    public void FireWebhooksTest() throws IOException {

        HttpPost method = new HttpPost(serverAddress +
            "/webhooks/callback_id");

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("callbackUrl", serverAddress + "/dummy"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        method.setEntity(entity);

        assertEquals(201, getStatus(method));

        HttpPost create_method = new HttpPost(serverAddress +
                "/objects/new");
        assertEquals(201, getStatus(create_method));

        try {
            for(int i = 0; i < 5; i++) {

                Thread.sleep(200);

                if(TestEndpoint.lastBody != null) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(TestEndpoint.lastBody);

        assertNotNull("Our webhook wasn't called!", TestEndpoint.lastBody);
        assertTrue("Our webhook didn't have the content we expected!", compile(
                "ingest", DOTALL).matcher(TestEndpoint.lastBody)
                .find());


    }
}
