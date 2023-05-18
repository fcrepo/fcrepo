/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.micrometer.core.annotation.Timed;
import org.fcrepo.config.SystemInfoConfig;


/**
 * Endpoint to display system info
 *
 * @author mikejritter
 * @since 5/1/2023
 */
@Timed
@Path("/fcr:systeminfo")
public class FedoraSystemInfo extends FedoraBaseResource {

    @Inject
    private SystemInfoConfig config;

    /**
     * JAX-RS entry
     */
    public FedoraSystemInfo() {
        super();
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response getSystemInfo() {
        final var systemInfo = Map.of("version", config.getImplementationVersion(),
                                      "commit", config.getGitCommit());
        return Response.ok().entity(systemInfo).build();
    }

}
