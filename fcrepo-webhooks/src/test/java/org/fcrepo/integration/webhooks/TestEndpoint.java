
package org.fcrepo.integration.webhooks;

import static javax.ws.rs.core.Response.ok;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Path("/dummy")
@Component
public class TestEndpoint {

    public static String lastBody;

    @POST
    public Response dummyWebhookEndpoint(final String body) {

        TestEndpoint.lastBody = body;
        return ok().build();
    }

}
