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

import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.shared.JenaException;

/**
 * The purpose of this class is to allow clients to manipulate the JCR
 * namespaces in play in a repository. This is necessary to allow the use of
 * traditional Fedora namespaced PIDs. Unlike Fedora Classic, a JCR requires
 * that namespaces be registered before use. The catalog of namespaces is very
 * simple, just a set of prefix-URI pairs.
 *
 * @author ajs6f
 */
@Scope("prototype")
@Path("/fcr:namespaces")
public class FedoraRepositoryNamespaces extends AbstractResource {

    @Inject
    protected Session session;

    /**
     * Register multiple object namespaces.
     *
     * @return 204
     * @throws RepositoryException
     */
    @POST
    @Timed
    @Consumes({contentTypeSPARQLUpdate})
    public Response updateNamespaces(final InputStream requestBodyStream)
        throws IOException {

        try {
            final UriAwareIdentifierConverter idTranslator =
                    new UriAwareIdentifierConverter(session, UriBuilder.fromResource(FedoraLdp.class));
            final Dataset dataset =
                repositoryService.getNamespaceRegistryDataset(session, idTranslator);
            parseExecute(IOUtils.toString(requestBodyStream), dataset);
            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
            return status(SC_NO_CONTENT).build();
        } catch ( JenaException ex ) {
            return status(SC_BAD_REQUEST).entity(ex.getMessage()).build();
        } finally {
            session.logout();
        }
    }

    /**
     * @return namespaces as RDF
     * @throws RepositoryException
     */
    @GET
    @Timed
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML, JSON_LD})
    @HtmlTemplate("jcr:namespaces")
    public RdfStream getNamespaces() {
        final UriAwareIdentifierConverter idTranslator
                = new UriAwareIdentifierConverter(session, UriBuilder.fromResource(FedoraLdp.class));
        return repositoryService.getNamespaceRegistryStream(session, idTranslator).session(session);
    }
}
