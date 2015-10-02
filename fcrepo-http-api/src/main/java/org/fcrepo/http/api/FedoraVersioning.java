/**
 * Copyright 2015 DuraSpace, Inc.
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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryVersionRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.VersionsRdfContext;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author cabeer
 * @since 9/25/14
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersioning extends FedoraBaseResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraVersioning.class);


    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected String externalPath;

    protected FedoraResource resource;


    /**
     * Default JAX-RS entry point
     */
    public FedoraVersioning() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraVersioning(final String externalPath) {
        this.externalPath = externalPath;
    }


    /**
     * Enable versioning
     * @return the response
     */
    @PUT
    public Response enableVersioning() {
        LOGGER.info("Enable versioning for '{}'", externalPath);
        resource().enableVersioning();

        try {
            session.save();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return created(uriInfo.getRequestUri()).build();
    }

    /**
     * Disable versioning
     * @return the response
     */
    @DELETE
    public Response disableVersioning() {
        LOGGER.info("Disable versioning for '{}'", externalPath);
        resource().disableVersioning();

        try {
            session.save();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return noContent().build();
    }

    /**
     * Create a new version checkpoint and tag it with the given label.  If
     * that label already describes another version it will silently be
     * reassigned to describe this version.
     *
     * @param slug the value of slug
     * @return response
     * @throws RepositoryException if repository exception occurred
     */
    @POST
    public Response addVersion(@HeaderParam("Slug") final String slug) throws RepositoryException {
        if (!isBlank(slug)) {
            LOGGER.info("Request to add version '{}' for '{}'", slug, externalPath);
            final String path = toPath(translator(), externalPath);
            versionService.createVersion(session, path, slug);
            return created(URI.create(nodeToResource(translator()).convert(
                            resource().getBaseVersion().getFrozenNode()).getURI())).build();
        }
        return status(BAD_REQUEST).entity("Specify label for version").build();
    }


    /**
     * Get the list of versions for the object
     *
     * @return List of versions for the object as RDF
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({TURTLE + ";qs=10", JSON_LD + ";qs=8", N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN,
            TURTLE_X, TEXT_HTML, APPLICATION_XHTML_XML, "*/*"})
    public RdfStream getVersionList() {
        if (!resource().isVersioned()) {
            throw new RepositoryVersionRuntimeException("This operation requires that the node be versionable");
        }

        return resource().getTriples(translator(), VersionsRdfContext.class)
                .session(session)
                .topic(translator().reverse().convert(resource()).asNode());
    }

    protected FedoraResource resource() {
        if (resource == null) {
            resource = getResourceFromPath(externalPath);
        }

        return resource;
    }

    @Override
    protected Session session() {
        return session;
    }
}
