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
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.riot.Lang.TTL;
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
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.jcr.ItemNotFoundException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
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

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
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
@Path("/{path: (.+/)?}fcr:acl")
public class FedoraAcl extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraAcl.class);

    public static final String ROOT_AUTHORIZATION_PROPERTY = "fcrepo.auth.webac.authorization";

    private static final String ROOT_AUTHORIZATION_LOCATION = "/root-authorization.ttl";

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
     */
    @PUT
    public Response createFedoraWebacAcl(@HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
                                         final InputStream requestBodyStream) {

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

            final MediaType contentType =
                getSimpleContentType(requestContentType == null ? RDFMediaType.TURTLE_TYPE : requestContentType);
            if (isRdfContentType(contentType.toString())) {

                try (final RdfStream resourceTriples =
                         created ? new DefaultRdfStream(asNode(aclResource)) : getResourceTriples(aclResource)) {
                    replaceResourceWithStream(aclResource, requestBodyStream, contentType, resourceTriples);

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
    public Response updateSparql(final InputStream requestBodyStream)
        throws IOException, ItemNotFoundException {
        hasRestrictedPath(externalPath);

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        final FedoraResource aclResource = resource().getAcl();

        if (aclResource == null) {
            if (resource().hasType(FEDORA_REPOSITORY_ROOT)) {
                throw new ClientErrorException("The default root ACL is system generated and cannot be modified. " +
                        "To override the default root ACL you must PUT a user-defined ACL to this endpoint.",
                        CONFLICT);
            }

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
     */
    @GET
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
                N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
                TURTLE_X, TEXT_HTML_WITH_CHARSET })
    public Response getResource(@HeaderParam("Range") final String rangeValue)
            throws IOException, ItemNotFoundException {

        LOGGER.info("GET resource '{}'", externalPath);

        final FedoraResource aclResource = resource().getAcl();

        if (aclResource == null) {
            if (resource().hasType(FEDORA_REPOSITORY_ROOT)) {
                final String resourceUri = getUri(resource()).toString();
                final String aclUri = resourceUri + (resourceUri.endsWith("/") ? "" : "/") + FCR_ACL;

                final RdfStream defaultRdfStream = DefaultRdfStream.fromModel(createResource(aclUri).asNode(),
                    getDefaultAcl(aclUri));
                final RdfNamespacedStream output = new RdfNamespacedStream(defaultRdfStream,
                    namespaceRegistry.getNamespaces());
                return ok(output).build();
            }

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
                if (resource().hasType(FEDORA_REPOSITORY_ROOT)) {
                    throw new ClientErrorException("The default root ACL is system generated and cannot be deleted. " +
                            "To override the default root ACL you must PUT a user-defined ACL to this endpoint.",
                            CONFLICT);
                }

                throw new ItemNotFoundException();
            }

            return noContent().build();

        } finally {
            lock.release();
        }
    }

    /**
     * Retrieve the default root ACL from a user specified location if it exists,
     * otherwise the one provided by Fedora will be used.
     * @param baseUri the URI of the default ACL
     * @return Model the rdf model of the default root ACL
     */
    public static Model getDefaultAcl(final String baseUri) {
        final String rootAcl = System.getProperty(ROOT_AUTHORIZATION_PROPERTY);
        final Model model = createDefaultModel();

        if (rootAcl != null && new File(rootAcl).isFile()) {
            try {
                LOGGER.debug("Getting root authorization from file: {}", rootAcl);

                RDFDataMgr.read(model, rootAcl, baseUri, null);

                return model;
            } catch (final JenaException ex) {
                throw new RuntimeException("Error parsing the default root ACL " + rootAcl + ".", ex);
            }
        }

        try (final InputStream is = FedoraAcl.class.getResourceAsStream(ROOT_AUTHORIZATION_LOCATION)) {
            LOGGER.debug("Getting root ACL from classpath: {}", ROOT_AUTHORIZATION_LOCATION);

            return model.read(is, baseUri, TTL.getName());
        } catch (final IOException ex) {
            throw new RuntimeException("Error reading the default root Acl " + ROOT_AUTHORIZATION_LOCATION + ".", ex);
        } catch (final JenaException ex) {
            throw new RuntimeException("Error parsing the default root ACL " + ROOT_AUTHORIZATION_LOCATION + ".", ex);
        }
    }
}
