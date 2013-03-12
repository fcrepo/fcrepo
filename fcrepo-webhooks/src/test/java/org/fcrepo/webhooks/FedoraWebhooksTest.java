package org.fcrepo.webhooks;


import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/rest.xml",
        "/spring-test/eventing.xml"})
public class FedoraWebhooksTest extends AbstractResourceTest {

    @Test
    public void registerWebhookCallbackTest() throws IOException {
        assertEquals(201, getStatus(new HttpPost(serverAddress +
                "/webhooks/callback_id?callbackUrl=info:fedora/fake:url")));


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
}
