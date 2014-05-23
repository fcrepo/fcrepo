/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api.repository;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * Expose node types at a REST endpoint
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/fcr:nodetypes")
public class FedoraRepositoryNodeTypes extends AbstractResource {

    @InjectedSession
    protected Session session;

    /**
     * Retrieve all node types as RDF
     * @return All node types as RDF
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML, JSON_LD})
    @Timed
    @HtmlTemplate("jcr:nodetypes")
    public RdfStream getNodeTypes(@Context final UriInfo uriInfo) throws RepositoryException {
        return nodeService.getNodeTypes(session).session(session);
    }

    /**
     * Update the NodeTypes by POSTing a CND file/fragment
     *
     * @return 201
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    @POST
    @Consumes({"text/cnd"})
    @Timed
    public Response updateCnd(final InputStream requestBodyStream)
        throws RepositoryException, IOException {

        try {
            nodeService.registerNodeTypes(session, requestBodyStream);

            return status(SC_NO_CONTENT).build();
        } catch ( RepositoryException ex ) {
            // this may be brittle, but the returned exception isn't an InvalideNodeTypeDefinitionException...
            if ( ex.getMessage().indexOf("Reading the node definitions from the"
                    + " supplied stream resulted in problems(s)") != -1 ) {
                return status(SC_BAD_REQUEST).entity(ex.getMessage()).build();
            } else {
                throw ex;
            }
        } finally {
            session.logout();
        }
    }
}
