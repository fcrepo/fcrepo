/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.integration;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.RepositoryException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
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

    private Response doRequest(final FedoraId id) throws RepositoryException {
        final URI location = URI.create(identifierConverter().toExternalId(id.getFullId()));
        return Response.created(location).build();
    }

    private HttpIdentifierConverter identifierConverter() {
        return new HttpIdentifierConverter(uriInfo.getBaseUriBuilder().clone().path(RootTestResource.class));
    }

}
