/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.integration;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class acts as the REST Resource endpoint against which integration tests are executed.
 * This is used instead of the real F4 REST API for two reasons:
 * - These integration tests are intended to test the AuthZ functionality, not the F4 REST API
 * - A circular dependency between fcrepo-auth-common &lt;--&gt; fcrepo-http-api is bad
 *
 * @author awoods
 * @since 2014-06-26
 */
@Scope("prototype")
@Path("/{path: .*}")
public class RootTestResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(RootTestResource.class);

    @GET
    public Response get(@PathParam("path") final String externalPath) {
        final FedoraId id = identifierConverter().pathToInternalId(externalPath);
        LOGGER.trace("GET: {}", id.getFullIdPath());
        return Response.ok().build();
    }

    @PUT
    public Response put(@PathParam("path") final String externalPath) throws Exception {
        final FedoraId id = identifierConverter().pathToInternalId(externalPath);
        LOGGER.trace("PUT: {}", id.getFullIdPath());
        return doRequest(id);
    }

    @POST
    public Response post(@PathParam("path") final String externalPath) throws Exception {
        final FedoraId id = identifierConverter().pathToInternalId(externalPath);
        LOGGER.trace("POST: {}", id.getFullIdPath());
        return doRequest(id);
    }

    private Response doRequest(final FedoraId id) {
        final URI location = URI.create(identifierConverter().toExternalId(id.getFullId()));
        return Response.created(location).build();
    }

    private HttpIdentifierConverter identifierConverter() {
        return new HttpIdentifierConverter(uriInfo.getBaseUriBuilder().clone().path(RootTestResource.class));
    }

}
