package org.fcrepo.integration.webhooks;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import static javax.ws.rs.core.Response.ok;

@Path("/dummy")
@Component
public class TestEndpoint {

    public static String lastBody;

    @POST
    public Response dummyWebhookEndpoint(String body) {

        TestEndpoint.lastBody = body;
        return ok().build();
    }

}
