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
package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.slf4j.LoggerFactory.getLogger;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * Run a fixity check on a path
 *
 * @author ajs6f
 * @since Jun 12, 2013
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:fixity")
public class FedoraFixity extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraFixity.class);

    @InjectedSession
    protected Session session;

    /**
     * Get the results of a fixity check for a path
     *
     * GET /path/to/some/datastream/fcr:fixity
     *
     * @param pathList
     * @param request
     * @param uriInfo
     * @return datastream fixity in the given format
     * @throws RepositoryException
     */
    @GET
    @Timed
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream getDatastreamFixity(@PathParam("path")
        final List<PathSegment> pathList,
        @Context
        final Request request,
        @Context
        final UriInfo uriInfo) throws RepositoryException {

        final String path = toPath(pathList);
        LOGGER.trace("Getting datastream fixity profile path {}", path);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);

        final Datastream ds = datastreamService.getDatastream(session, jcrPath);
        return datastreamService.getFixityResultsModel(subjects, ds).session(session);

    }
}
