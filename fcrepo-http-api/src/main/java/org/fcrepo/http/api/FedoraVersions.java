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

package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
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
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Endpoint for managing versions of nodes
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersions extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraNodes.class);

    /**
     * Get the list of versions for the object
     *
     * @param pathList
     * @param request
     * @param uriInfo
     * @return
     * @throws RepositoryException
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, RDF_JSON, NTRIPLES,
            TEXT_HTML})
    public RdfStream getVersionList(@PathParam("path")
            final List<PathSegment> pathList,
            @Context
            final Request request,
            @Context
            final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);

        LOGGER.trace("Getting versions list for: {}", path);

        final FedoraResource resource = nodeService.getObject(session, path);

        return resource.getVersionTriples(nodeTranslator()).session(session).topic(
                nodeTranslator().getGraphSubject(resource.getNode()).asNode());
    }

    /**
     * Create a new version checkpoint and tag it with the given label.  If
     * that label already describes another version it will silently be
     * reassigned to describe this version.
     *
     * @param pathList
     * @param versionLabel
     * @return
     * @throws RepositoryException
     */
    @POST
    @Path("/{versionLabel}")
    public Response addVersion(@PathParam("path")
            final List<PathSegment> pathList,
            @PathParam("versionLabel")
            final String versionLabel) throws RepositoryException {
        return addVersion(toPath(pathList), versionLabel);
    }

    /**
     * Create a new version checkpoint with no label.
     */
    @POST
    public Response addVersion(@PathParam("path")
            final List<PathSegment> pathList) throws RepositoryException {
        return addVersion(toPath(pathList), null);
    }

    private Response addVersion(final String path, final String label) throws RepositoryException {
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);
            versionService.createVersion(session.getWorkspace(),
                    Collections.singleton(path));
            if (label != null) {
                resource.addVersionLabel(label);
            }
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    /**
     * Retrieve the tagged version of an object.
     * @param pathList
     * @param versionLabel
     * @param uriInfo
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @Path("/{versionLabel}")
    @GET
    @Produces({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, RDF_JSON, NTRIPLES})
    public RdfStream getVersion(@PathParam("path")
            final List<PathSegment> pathList,
            @PathParam("versionLabel")
            final String versionLabel,
            @Context
            final Request request,
            @Context
            final UriInfo uriInfo) throws RepositoryException, IOException {
        final String path = toPath(pathList);
        LOGGER.trace("Getting version profile for: {} at version: {}", path,
                versionLabel);

        final FedoraResource resource =
            nodeService.getObject(session, path, versionLabel);

        if (resource == null) {
            throw new WebApplicationException(status(NOT_FOUND).build());
        } else {
            return resource.getTriples(nodeTranslator()).session(session).topic(
                    nodeTranslator().getGraphSubject(resource.getNode()).asNode());
        }
    }

    /**
     * A translator suitable for subjects that represent nodes.
     */
    protected GraphSubjects nodeTranslator() {
        return new HttpGraphSubjects(session, FedoraNodes.class, uriInfo);
    }

}
