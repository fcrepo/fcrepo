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
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.api.versioning.VersionAwareHttpIdentifierTranslator;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Endpoint for managing versions of nodes
 *
 * @author awoods
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersions extends ContentExposingResource {

    @InjectedSession
    protected Session session;

    @Autowired
    private SessionFactory sessionFactory = null;

    private static final Logger LOGGER = getLogger(FedoraVersions.class);

    /**
     * Get the list of versions for the object
     *
     * @param pathList
     * @param request
     * @param uriInfo
     * @return List of versions for the object as RDF
     * @throws RepositoryException
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream getVersionList(@PathParam("path") final List<PathSegment> pathList,
            @Context final Request request,
            @Context final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);

        LOGGER.info("Getting versions list for: {}", path);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.info("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);

        final FedoraResource resource = nodeService.getObject(session, jcrPath);

        try {
            return resource.getVersionTriples(nodeTranslator()).session(session).topic(
                    nodeTranslator().getSubject(resource.getNode().getPath()).asNode());
        } catch ( final UnsupportedRepositoryOperationException ex ) {
            throw new WebApplicationException( status(NOT_FOUND).entity("This resource is not versioned").build() );
        }
    }

    /**
     * Create a new version checkpoint and tag it with the given label.  If
     * that label already describes another version it will silently be
     * reassigned to describe this version.
     *
     * @param pathList
     * @param label
     * @return response
     * @throws RepositoryException
     */
    @POST
    @Path("/{label:.+}")
    public Response addVersion(@PathParam("path")
            final List<PathSegment> pathList,
            @PathParam("label")
            final String label) throws RepositoryException {
        return addVersion(toPath(pathList), label);
    }

    /**
     * Reverts the resource at the given path to the version specified by
     * the label.
     * @param pathList
     * @param label
     * @return response
     * @throws RepositoryException
     */
    @PATCH
    @Path("/{label:.+}")
    public Response revertToVersion(@PathParam("path") final List<PathSegment> pathList,
                                    @PathParam("label") final String label) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.info("Reverting {} to version {}.", path,
                label);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.info("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        try {
            versionService.revertToVersion(session.getWorkspace(), jcrPath, label);
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    /**
     * Removes the version specified by the label.
     * @param pathList The resource the version is associated with.
     * @param label The version label
     * @return 204 No Content
     * @throws RepositoryException
    **/
    @DELETE
    @Path("/{label:.+}")
    public Response removeVersion(@PathParam("path") final List<PathSegment> pathList,
            @PathParam("label") final String label) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.info("Removing {} version {}.", path, label);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.info("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        try {
            versionService.removeVersion(session.getWorkspace(), jcrPath, label);
            return noContent().build();
        } catch ( final VersionException ex ) {
            return status(BAD_REQUEST).entity(ex.getMessage()).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Create a new version checkpoint with no label.
     * @return response
     */
    @POST
    public Response addVersion(@PathParam("path")
            final List<PathSegment> pathList) throws RepositoryException {
        return addVersion(toPath(pathList), null);
    }

    private Response addVersion(final String path, final String label) throws RepositoryException {
        try {
            LOGGER.info("Adding {} version {}.", path, label);
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.info("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            final FedoraResource resource =
                    nodeService.getObject(session, jcrPath);
            versionService.createVersion(session.getWorkspace(),
                    Collections.singleton(jcrPath));
            if (label != null) {
                resource.addVersionLabel(label);
            }
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    /**
     * Retrieve a version of an object.  The path structure is as follows
     * (though these URLs are returned from getVersionList and need not be
     * constructed manually):
     * /versionable-node/fcr:versions/label/path/to/any/copied/unversionable/nodes
     * @param pathList
     * @param label the label for the version of the subgraph
     * @param uriInfo
     * @return the version of the object as RDF in the requested format
     * @throws RepositoryException
     */
    @Path("/{label:.+}")
    @GET
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream getVersion(@PathParam("path")
            final List<PathSegment> pathList,
            @PathParam("label")
            final String label,
            @Context
            final Request request, @Context final HttpServletResponse servletResponse,
            @Context
            final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.trace("Getting version profile for: {} at version: {}", path,
                label);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, this.getClass(), uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        final Node node = nodeTranslator().getNodeFromGraphSubjectForVersionNode(uriInfo.getRequestUri().toString());
        if (node == null) {
            throw new WebApplicationException(status(NOT_FOUND).build());
        }
        final FedoraResource resource = new FedoraResourceImpl(node);
        checkCacheControlHeaders(request, servletResponse, resource);
        return resource.getTriples(nodeTranslator()).session(session).topic(
                nodeTranslator().getSubject(resource.getNode().getPath()).asNode());
    }

    /**
     * Get the binary content of a historic version of a datastream.
     * @see FedoraContent#getContent
     * @param pathList
     * @return Binary blob
     * @throws RepositoryException
     */
    @Path("/{label:.+}/fcr:content")
    @GET
    @Timed
    public Response getHistoricContent(@PathParam("path")
                                       final List<PathSegment> pathList, @HeaderParam("Range")
                                       final String rangeValue, @Context
                                       final Request request,
                                       @Context final HttpServletResponse servletResponse
    ) throws RepositoryException, IOException {
        try {
            LOGGER.info("Attempting get of {}.", uriInfo.getRequestUri());
            final Node frozenNode = nodeTranslator().getNodeFromGraphSubjectForVersionNode(
                    uriInfo.getRequestUri().toString().replace("/" + FCR_CONTENT, ""));
            final Datastream ds =
                    datastreamService.asDatastream(frozenNode);
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class,
                            uriInfo);
            return getDatastreamContentResponse(ds, rangeValue, request, servletResponse, subjects);

        } finally {
            session.logout();
        }
    }

    /**
     * A translator suitable for subjects that represent nodes.
     */
    protected VersionAwareHttpIdentifierTranslator nodeTranslator() throws RepositoryException {
        return new VersionAwareHttpIdentifierTranslator(session,
                sessionFactory.getInternalSession(), FedoraNodes.class,
                uriInfo);
    }

}
