package org.fcrepo.webhooks;


import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/rest.xml",
        "/spring-test/eventing.xml"})
public class FedoraWebhooksTest extends AbstractResourceTest {

    @Test
    public void registerWebhookCallbackTest() throws IOException {
        assertEquals(201, getStatus(new HttpPost(serverAddress +
                "/webhooks/callback_id?callbackUrl=info:fedora")));

    }
}
