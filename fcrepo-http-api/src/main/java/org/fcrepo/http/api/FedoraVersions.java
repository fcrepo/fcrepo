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

import com.google.common.annotations.VisibleForTesting;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singleton;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Endpoint for managing versions of nodes
 *
 * @author awoods
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions/{labelAndOptionalPathIntoVersion: .*}")
public class FedoraVersions extends ContentExposingResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraVersions.class);

    @PathParam("path") protected List<PathSegment> pathList;
    @PathParam("labelAndOptionalPathIntoVersion") protected List<PathSegment> pathListIntoVersion;

    protected String path;
    protected String label;
    protected String pathIntoVersion;

    protected FedoraResource resource;

    /**
     * Default JAX-RS entry point
     */
    public FedoraVersions() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param path
     */
    @VisibleForTesting
    public FedoraVersions(final String path, final String label, final String pathIntoVersion) {
        this.path = path;
        this.label = label;
        this.pathIntoVersion = pathIntoVersion;
    }

    @PostConstruct
    private void postConstruct() {
        this.path = toPath(pathList);
        this.label = pathListIntoVersion.get(0).getPath();
        this.pathIntoVersion = "." + toPath(pathListIntoVersion.subList(1, pathListIntoVersion.size()));
    }

    /**
     * Create a new version checkpoint and tag it with the given label.  If
     * that label already describes another version it will silently be
     * reassigned to describe this version.
     *
     * @return response
     * @throws RepositoryException
     */
    @POST
    public Response addVersion() throws RepositoryException {
        try {
            final Collection<String> versions = versionService.createVersion(session.getWorkspace(),
                    singleton(path));
            if (label != null) {
                unversionedResource().addVersionLabel(label);
            }

            final String version = (label != null) ? label : versions.iterator().next();
            return noContent().header("Location", translator().getSubject(
                    path) + "/fcr:versions/" + version).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Reverts the resource at the given path to the version specified by
     * the label.
     * @return response
     * @throws RepositoryException
     */
    @PATCH
    public Response revertToVersion() throws RepositoryException {
        LOGGER.info("Reverting {} to version {}.", path,
                label);
        try {
            versionService.revertToVersion(session.getWorkspace(), path, label);
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    /**
     * Removes the version specified by the label.
     * @return 204 No Content
     * @throws RepositoryException
    **/
    @DELETE
    public Response removeVersion() throws RepositoryException {
        LOGGER.info("Removing {} version {}.", path, label);
        try {
            versionService.removeVersion(session.getWorkspace(), path, label);
            return noContent().build();
        } catch ( VersionException ex ) {
            return status(BAD_REQUEST).entity(ex.getMessage()).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Retrieve a version of an object.  The path structure is as follows
     * (though these URLs are returned from getVersionList and need not be
     * constructed manually):
     * /versionable-node/fcr:versions/label/path/to/any/copied/unversionable/nodes
     * @return the version of the object as RDF in the requested format
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE + ";qs=10", JSON_LD + ";qs=8",
            N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML, "*/*"})
    public Response getVersion(@HeaderParam("Prefer") final Prefer prefer,
                               @HeaderParam("Range") final String rangeValue) throws RepositoryException, IOException {
        LOGGER.trace("Getting version profile for: {} at version: {}", path,
                label);
        checkCacheControlHeaders(request, servletResponse, resource(), session);
        final RdfStream rdfStream = new RdfStream().session(session).topic(
                translator().getSubject(unversionedResource().getPath()).asNode());
        return getContent(prefer, rangeValue, rdfStream);
    }

    @VisibleForTesting
    protected FedoraResource unversionedResource() {
        return getResourceFromPath();
    }

    @Override
    protected FedoraResource resource() {

        try {
            if (resource == null) {
                final FedoraResource fedoraResource = unversionedResource();

                final Node frozenNode = fedoraResource.getNodeVersion(label);

                if (frozenNode == null) {
                    throw new NotFoundException();
                }

                final Node node;

                if (pathIntoVersion != null) {
                    node = frozenNode.getNode(pathIntoVersion);
                } else {
                    node = frozenNode;
                }

                final boolean metadata = pathListIntoVersion != null &&
                        pathListIntoVersion.get(pathListIntoVersion.size() - 1).getPath().equals(FCR_METADATA);

                if (DatastreamImpl.hasMixin(node)) {
                    final DatastreamImpl datastream = new DatastreamImpl(node);

                    if (metadata) {
                        resource = datastream;
                    } else {
                        resource = datastream.getBinary();
                    }
                } else if (FedoraBinaryImpl.hasMixin(node)) {
                    resource = new FedoraBinaryImpl(node);
                } else {
                    resource = new FedoraObjectImpl(node);
                }
            }

            return resource;

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    void addResourceHttpHeaders(final FedoraResource resource) {
        // no-op
    }

    @Override
    String path() {
        return path;
    }

    @Override
    List<PathSegment> pathList() {
        return pathList;
    }


    @Override
    Session session() {
        return session;
    }
}
