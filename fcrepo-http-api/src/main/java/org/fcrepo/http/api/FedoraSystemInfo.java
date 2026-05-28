/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

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
