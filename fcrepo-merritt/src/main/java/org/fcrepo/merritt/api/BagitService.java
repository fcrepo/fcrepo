package org.fcrepo.merritt.api;

import org.fcrepo.AbstractResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/bagit/{node}/{object}/{version}")
public class BagitService extends AbstractResource {

    @GET
    public Response getBagitVersionOfObject() {
        return ok("").build();
    }
}
