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

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.ResultSet;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.responses.ViewHelpers;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.impl.NamespaceRdfContext;
import org.fcrepo.kernel.utils.LogoutCallback;
import org.fcrepo.transform.http.responses.ResultSetStreamingOutput;
import org.fcrepo.transform.sparql.JQLConverter;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

import static com.google.common.util.concurrent.Futures.addCallback;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.ok;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeResultsBIO;
import static org.apache.jena.riot.WebContent.contentTypeResultsJSON;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.apache.jena.riot.WebContent.contentTypeSSE;
import static org.apache.jena.riot.WebContent.contentTypeTextCSV;
import static org.apache.jena.riot.WebContent.contentTypeTextPlain;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_SPARQL_RDF_VARIANTS;
import static org.fcrepo.http.commons.responses.BaseHtmlProvider.templateFilenameExtension;
import static org.fcrepo.http.commons.responses.BaseHtmlProvider.templatesLocation;
import static org.fcrepo.http.commons.responses.BaseHtmlProvider.velocityPropertiesLocation;
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
     * A stub method so we can return a text/html representation using
     * the right template.
     *
     * @return
     * @throws RepositoryException
     */
    @GET
    @Timed
    @Produces({TEXT_HTML})
    public Response sparqlQueryForm() throws IOException, RepositoryException {

        final Properties properties = new Properties();
        final URL propertiesUrl =
            getClass().getResource(velocityPropertiesLocation);
        final VelocityEngine velocity = new VelocityEngine();
        LOGGER.debug("Using Velocity configuration from {}", propertiesUrl);
        try (final InputStream propertiesStream = propertiesUrl.openStream()) {
            properties.load(propertiesStream);
        }
        velocity.init(properties);
        final Template template =
            velocity.getTemplate(templatesLocation + "/search-sparql"
                    + templateFilenameExtension);
        final org.apache.velocity.context.Context context =
            new VelocityContext();
        context.put("uriInfo", uriInfo);
        context.put("model", new NamespaceRdfContext(session).asModel());
        context.put("helpers", ViewHelpers.getInstance());

        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream output) throws IOException {

                final Writer outWriter = new OutputStreamWriter(output);
                template.merge(context, outWriter);
                outWriter.flush();
            }
        };

        return ok(stream).build();
    }

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
    @Produces({contentTypeTextTSV, contentTypeTextCSV, contentTypeSSE,
            contentTypeTextPlain, contentTypeResultsJSON,
            contentTypeResultsXML, contentTypeResultsBIO, contentTypeTurtle,
            contentTypeN3, contentTypeNTriples, contentTypeRDFXML})
    public Response runSparqlQuery(final InputStream requestBodyStream,
        @Context final Request request, @Context final UriInfo uriInfo)
        throws IOException, RepositoryException {

        final IdentifierTranslator graphSubjects = new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

        final Variant bestPossibleResponse =
            request.selectVariant(POSSIBLE_SPARQL_RDF_VARIANTS);

        final String sparqlQuery = IOUtils.toString(requestBodyStream);

        LOGGER.trace("Running SPARQL query: {}", sparqlQuery);

        final JQLConverter jqlConverter = new JQLConverter(session, graphSubjects, sparqlQuery);

        LOGGER.trace("Converted to JQL query: {}", jqlConverter.getStatement());

        final ResultSet resultSet = jqlConverter.execute();

        final ResultSetStreamingOutput streamingOutput =
            new ResultSetStreamingOutput(resultSet, bestPossibleResponse
                    .getMediaType());

        addCallback(streamingOutput, new LogoutCallback(session));

        return ok(streamingOutput).build();
    }
}
