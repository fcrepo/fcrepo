/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.transform.http;

import com.hp.hpl.jena.query.ResultSet;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.WebContent;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.LogoutCallback;
import org.fcrepo.transform.http.responses.ResultSetStreamingOutput;
import org.fcrepo.transform.sparql.JQLConverter;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.util.concurrent.Futures.addCallback;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_SPARQL_RDF_VARIANTS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Primitive SPARQL JAX-RS endpoint
 *
 * @author cabeer
 */
@Component
@Scope("prototype")
@Path("/fcr:sparql")
public class FedoraSparql extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraSparql.class);

    /**
     * Execute a SPARQL query against the JCR index
     * @param requestBodyStream
     * @param uriInfo
     * @return
     * @throws IOException
     * @throws RepositoryException
     */
    @POST
    @Consumes({contentTypeSPARQLQuery})
    @Produces({WebContent.contentTypeTextTSV,
                  WebContent.contentTypeTextCSV, WebContent.contentTypeSSE,
                  WebContent.contentTypeTextPlain,
                  WebContent.contentTypeResultsJSON,
                  WebContent.contentTypeResultsXML,
                  WebContent.contentTypeResultsBIO,
                  WebContent.contentTypeTurtle, WebContent.contentTypeN3,
                  WebContent.contentTypeNTriples, WebContent.contentTypeRDFXML})
    public Response runSparqlQuery(final InputStream requestBodyStream,
                                   @Context final Request request,
                                   @Context final UriInfo uriInfo) throws IOException, RepositoryException {

        final GraphSubjects graphSubjects = new HttpGraphSubjects(session, FedoraNodes.class, uriInfo);

        final Variant bestPossibleResponse =
            request.selectVariant(POSSIBLE_SPARQL_RDF_VARIANTS);

        final String sparqlQuery = IOUtils.toString(requestBodyStream);

        LOGGER.trace("Running SPARQL query: {}", sparqlQuery);

        final JQLConverter jqlConverter = new JQLConverter(session, graphSubjects, sparqlQuery);

        LOGGER.trace("Converted to JQL query: {}", jqlConverter.getStatement());

        final ResultSet resultSet = jqlConverter.execute();

        ResultSetStreamingOutput streamingOutput = new ResultSetStreamingOutput(resultSet, bestPossibleResponse.getMediaType());

        addCallback(streamingOutput, new LogoutCallback(session));

        return Response.ok(streamingOutput).build();
    }
}
