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

package org.fcrepo.http.api.repository;

import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
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
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;

/**
 * The purpose of this class is to allow clients to manipulate the JCR
 * namespaces in play in a repository. This is necessary to allow the use of
 * traditional Fedora namespaced PIDs. Unlike Fedora Classic, a JCR requires
 * that namespaces be registered before use. The catalog of namespaces is very
 * simple, just a set of prefix-URI pairs.
 *
 * @author ajs6f
 */
@Component
@Scope("prototype")
@Path("/fcr:namespaces")
public class FedoraRepositoryNamespaces extends AbstractResource {

    @InjectedSession
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
        throws RepositoryException, IOException {

        try {
            final Dataset dataset =
                nodeService.getNamespaceRegistryDataset(session);
            parseExecute(IOUtils.toString(requestBodyStream), dataset);
            session.save();
            return status(SC_NO_CONTENT).build();
        } finally {
            session.logout();
        }
    }

    /**
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @GET
    @Timed
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML})
    @HtmlTemplate("jcr:namespaces")
    public RdfStream getNamespaces() throws RepositoryException, IOException {
        return nodeService.getNamespaceRegistryStream(session).session(session);
    }
}
