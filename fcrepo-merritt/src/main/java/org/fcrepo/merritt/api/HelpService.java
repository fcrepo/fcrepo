package org.fcrepo.merritt.api;


import org.fcrepo.AbstractResource;

import static javax.ws.rs.core.Response.ok;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/help")
public class HelpService extends AbstractResource {
    @GET
    public Response getHelp() {
        return ok("I am a help service!").build();
    }
}
