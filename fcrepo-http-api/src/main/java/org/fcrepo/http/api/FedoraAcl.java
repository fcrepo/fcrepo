/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_WEBAC_ACL;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.IOUtils;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * @author lsitu
 * @author peichman
 * @since 4/20/18
 */
@Scope("request")
@Path("/{path: .*}/fcr:acl")
public class FedoraAcl extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraAcl.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected String externalPath;

    /**
     * Default JAX-RS entry point
     */
    public FedoraAcl() {
        super();
    }

    /**
     * PUT to create FedoraWebacACL resource.
     *
     * @return the response for a request to create a Fedora WebAc acl
     * @throws ConstraintViolationException in case this action would violate a constraint on repository structure
     */
    @PUT
    public Response createFedoraWebacAcl() throws ConstraintViolationException {
        if (resource().hasType(FEDORA_WEBAC_ACL) || resource().isMemento()) {
            throw new BadRequestException("ACL resource creation is not allowed for resource " + resource().getPath());
        }

        final boolean created;
        final FedoraResource aclResource;

        final String path = toPath(translator(), externalPath);
        final AcquiredLock lock = lockManager.lockForWrite(path, session.getFedoraSession(), nodeService);
        try {
            LOGGER.info("PUT acl resource '{}'", externalPath);

            aclResource = resource().findOrCreateAcl();
            created = aclResource.isNew();
            session.commit();
        } finally {
            lock.release();
        }

        addCacheControlHeaders(servletResponse, resource, session);
        final URI location = getUri(aclResource);
        if (created) {
            return created(location).build();
        } else {
            return noContent().location(location).build();
        }
    }

    /**
     * PATCH to update an FedoraWebacACL resource using SPARQL-UPDATE
     *
     * @param requestBodyStream the request body stream
     * @return 204
     * @throws IOException if IO exception occurred
     */
    @PATCH
    @Consumes({ contentTypeSPARQLUpdate })
    @Timed
    public Response updateSparql(@ContentLocation final InputStream requestBodyStream)
            throws IOException {
        hasRestrictedPath(externalPath);

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        if (resource() instanceof FedoraBinary) {
            throw new BadRequestException(resource().getPath() + " is not a valid object to receive a PATCH");
        }

        final AcquiredLock lock = lockManager.lockForWrite(resource().getPath(), session.getFedoraSession(),
                nodeService);

        try {
            final String requestBody = IOUtils.toString(requestBodyStream, UTF_8);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            try (final RdfStream resourceTriples =
                    resource().isNew() ? new DefaultRdfStream(asNode(resource())) : getResourceTriples()) {
                LOGGER.info("PATCH for '{}'", externalPath);
                patchResourcewithSparql(resource(), requestBody, resourceTriples);
            }
            session.commit();

            addCacheControlHeaders(servletResponse, resource(), session);

            return noContent().build();
        } catch (final IllegalArgumentException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (final AccessDeniedException e) {
            throw e;
        } catch (final RuntimeException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof PathNotFoundRuntimeException) {
                // the sparql update referred to a repository resource that doesn't exist
                throw new BadRequestException(cause.getMessage());
            }
            throw ex;
        } finally {
            lock.release();
        }
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

    /**
     * GET to retrieve the ACL resource.
     *
     * @param rangeValue the range value
     * @return a binary or the triples for the specified node
     * @throws IOException if IO exception occurred
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     * @throws UnsupportedAccessTypeException if unsupported access-type occurred
     */
    @GET
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
        N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
        TURTLE_X, TEXT_HTML_WITH_CHARSET })
    public Response getResource(@HeaderParam("Range") final String rangeValue)
            throws IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        final FedoraResource aclResource = resource().getAcl();

        if (aclResource == null) {
            return notFound();
        }

        checkCacheControlHeaders(request, servletResponse, aclResource, session);

        LOGGER.info("GET resource '{}'", externalPath);
        final AcquiredLock readLock = lockManager.lockForRead(aclResource.getPath());
        try (final RdfStream rdfStream = new DefaultRdfStream(asNode(aclResource))) {

            addResourceHttpHeaders(aclResource);
            return getContent(rangeValue, getChildrenLimit(), rdfStream);

        } finally {
            readLock.release();
        }
    }

    /**
     * Deletes an object.
     *
     * @return response
     */
    @DELETE
    @Timed
    public Response deleteObject() {

        hasRestrictedPath(externalPath);
        LOGGER.info("Delete resource '{}'", externalPath);

        final AcquiredLock lock = lockManager.lockForDelete(resource().getPath());

        try {
            final FedoraResource aclResource = resource().getAcl();
            if (aclResource != null) {
                aclResource.delete();
            }
            session.commit();

            if (aclResource != null) {
                return noContent().build();
            } else {
                return notFound();
            }
        } finally {
            lock.release();
        }
    }

    private Response notFound() {
        return status(NOT_FOUND).entity("Resource does not exist.").build();
    }
}
