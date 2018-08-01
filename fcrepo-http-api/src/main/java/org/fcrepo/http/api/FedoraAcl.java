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
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.jcr.ItemNotFoundException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.IOUtils;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
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
     * @param requestContentType The content type of the resource body
     * @param requestBodyStream  The request body as stream
     * @return the response for a request to create a Fedora WebAc acl
     * @throws ConstraintViolationException in case this action would violate a constraint on repository structure
     */
    @PUT
    public Response createFedoraWebacAcl(@HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
                                         final InputStream requestBodyStream)
        throws ConstraintViolationException {

        if (resource().isAcl() || resource().isMemento()) {
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

            final MediaType contentType = requestContentType == null ? RDFMediaType.TURTLE_TYPE : requestContentType;
            if (isRdfContentType(contentType.toString())) {

                try (final RdfStream resourceTriples =
                         created ? new DefaultRdfStream(asNode(aclResource)) : getResourceTriples(aclResource)) {
                    replaceResourceWithStream(aclResource, requestBodyStream, contentType, resourceTriples);

                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new BadRequestException("Content-Type (" + requestContentType + ") is invalid. Try text/turtle " +
                                              "or other RDF compatible type.");
            }
            session.commit();
        } finally {
            lock.release();
        }

        addCacheControlHeaders(servletResponse, aclResource, session);
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
    public Response updateSparql(final InputStream requestBodyStream)
        throws IOException, ItemNotFoundException {
        hasRestrictedPath(externalPath);

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        final FedoraResource aclResource = resource().getAcl();

        if (aclResource == null) {
            throw new ItemNotFoundException();
        }

        final AcquiredLock lock = lockManager.lockForWrite(aclResource.getPath(), session.getFedoraSession(),
                                                           nodeService);

        try {
            final String requestBody = IOUtils.toString(requestBodyStream, UTF_8);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, aclResource, session);

            try (final RdfStream resourceTriples =
                     aclResource.isNew() ? new DefaultRdfStream(asNode(aclResource)) :
                     getResourceTriples(aclResource)) {
                LOGGER.info("PATCH for '{}'", externalPath);
                patchResourcewithSparql(aclResource, requestBody, resourceTriples);
            }
            session.commit();

            addCacheControlHeaders(servletResponse, aclResource, session);

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
     * @throws UnsupportedAlgorithmException  if unsupported digest algorithm occurred
     * @throws UnsupportedAccessTypeException if unsupported access-type occurred
     */
    @GET
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
                N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
                TURTLE_X, TEXT_HTML_WITH_CHARSET })
    public Response getResource(@HeaderParam("Range") final String rangeValue)
            throws IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException, ItemNotFoundException {

        final FedoraResource aclResource = resource().getAcl();

        if (aclResource == null) {
            throw new ItemNotFoundException();
        }

        checkCacheControlHeaders(request, servletResponse, aclResource, session);

        LOGGER.info("GET resource '{}'", externalPath);
        final AcquiredLock readLock = lockManager.lockForRead(aclResource.getPath());
        try (final RdfStream rdfStream = new DefaultRdfStream(asNode(aclResource))) {

            addResourceHttpHeaders(aclResource);
            return getContent(rangeValue, getChildrenLimit(), rdfStream, aclResource);

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
    public Response deleteObject() throws ItemNotFoundException {

        hasRestrictedPath(externalPath);
        LOGGER.info("Delete resource '{}'", externalPath);

        final AcquiredLock lock = lockManager.lockForDelete(resource().getPath());

        try {
            final FedoraResource aclResource = resource().getAcl();
            if (aclResource != null) {
                aclResource.delete();
            }
            session.commit();

            if (aclResource == null) {
                throw new ItemNotFoundException();
            }

            return noContent().build();

        } finally {
            lock.release();
        }
    }

}
