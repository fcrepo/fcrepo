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

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.Lock;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK_TOKEN;
import static org.fcrepo.kernel.RdfLexicon.IS_DEEP;
import static org.fcrepo.kernel.RdfLexicon.LOCKS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Mike Durbin
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:lock")
public class FedoraLocks extends AbstractResource implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraLocks.class);

    @InjectedSession
    protected Session session;

    /**
     * Gets a description of the lock resource.
     */
    @GET
    @HtmlTemplate(value = "fcr:lock")
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream getLock(@PathParam("path") final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);
        LOGGER.trace("Getting lock profile for: {}", path);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        final Node node = session.getNode(jcrPath);
        final Lock lock = lockService.getLock(session, jcrPath);
        return getLockRdfStream(node, lock);
    }

    /**
     * Creates a lock at the given path that is tied to the current session.
     * Only the current session may make changes to the current path while
     * the lock is held.
     * @param isDeep if true the created lock will affect all nodes in the subgraph
     *               below
     */
    @POST
    @Timed
    public Response createLock(@PathParam("path") final List<PathSegment> pathList,
                               @QueryParam("deep") @DefaultValue("false") final boolean isDeep)
        throws RepositoryException, URISyntaxException {
        try {
            final String path = toPath(pathList);
            LOGGER.trace("Creating lock for: {}", path);
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            final Node node = session.getNode(jcrPath);
            final Lock lock = lockService.acquireLock(session, jcrPath, isDeep);
            session.save();
            final String location = getTranslator().getSubject(node.getPath()).getURI();
            LOGGER.debug("Locked {} with lock token {}.", jcrPath, lock.getLockToken());
            return created(new URI(location)).entity(location).header("Lock-Token", lock.getLockToken()).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Deletes a lock at the given path, freeing up other sessions to make
     * changes to it or its descendants.
     * @return 204
     */
    @DELETE
    @Timed
    public Response deleteLock(@PathParam("path") final List<PathSegment> pathList)
        throws RepositoryException, URISyntaxException {
        try {
            final String path = toPath(pathList);
            LOGGER.trace("Deleting lock for: {}", path);
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            lockService.releaseLock(session, jcrPath);
            session.save();
            LOGGER.debug("Unlocked {}.", jcrPath);
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    private HttpIdentifierTranslator getTranslator() {
        return new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
    }

    private RdfStream getLockRdfStream(final Node node, final Lock lock) throws RepositoryException {
        final HttpIdentifierTranslator translator = getTranslator();
        final com.hp.hpl.jena.graph.Node nodeSubject = translator.getSubject(node.getPath()).asNode();
        final com.hp.hpl.jena.graph.Node lockSubject = createURI(nodeSubject.getURI() + "/" + FCR_LOCK);

        final Triple[] lockTriples;
        final Triple locksT = create(lockSubject, LOCKS.asNode(), nodeSubject);
        final Triple isDeepT = create(lockSubject, IS_DEEP.asNode(),
                createLiteral(String.valueOf(lock.isDeep()), "", XSDDatatype.XSDboolean));
        if (lock.getLockToken() != null) {
            lockTriples = new Triple[] {
                locksT,
                isDeepT,
                create(lockSubject, HAS_LOCK_TOKEN.asNode(), createLiteral(lock.getLockToken()))};
        } else {
            lockTriples = new Triple[] {
                locksT, isDeepT };
        }
        return new RdfStream(lockTriples).topic(lockSubject).session(session);
    }

}
