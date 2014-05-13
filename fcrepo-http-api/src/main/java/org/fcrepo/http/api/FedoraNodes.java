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

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.sun.jersey.api.Responses.clientError;
import static com.sun.jersey.api.Responses.conflict;
import static com.sun.jersey.api.Responses.notAcceptable;
import static com.sun.jersey.api.Responses.notFound;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang.ArrayUtils.contains;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.RdfLexicon.FIRST_PAGE;
import static org.fcrepo.kernel.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.MOVE;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.COPY;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.openrdf.util.iterators.Iterators;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.ReferentialIntegrityException;

/**
 * CRUD operations on Fedora Nodes
 *
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}")
public class FedoraNodes extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraNodes.class);

    /**
     * Retrieve the node headers
     * @param pathList
     * @param request
     * @param servletResponse
     * @param uriInfo
     * @return
     * @throws RepositoryException
     */
    @HEAD
    @Timed
    public Response head(@PathParam("path") final List<PathSegment> pathList,
                     @Context final Request request,
                     @Context final HttpServletResponse servletResponse,
                     @Context final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.trace("Getting head for: {}", path);

        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, this.getClass(), uriInfo);

        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.trace("Head: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        final FedoraResource resource = nodeService.getObject(session, jcrPath);

        checkCacheControlHeaders(request, servletResponse, resource);

        addResourceHttpHeaders(servletResponse, resource, subjects);

        return status(OK).build();
    }


    /**
     * Retrieve the node profile
     *
     * @param pathList
     * @param offset with limit, control the pagination window of details for
     *        child nodes
     * @param limit with offset, control the pagination window of details for
     *        child nodes
     * @param request
     * @param uriInfo
     * @return
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream describe(@PathParam("path") final List<PathSegment> pathList,
            @QueryParam("offset") @DefaultValue("0") final int offset,
            @QueryParam("limit")  @DefaultValue("-1") final int limit,
            @HeaderParam("Prefer") final Prefer prefer,
            @Context final Request request,
            @Context final HttpServletResponse servletResponse,
            @Context final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.trace("Getting profile for: {}", path);
        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, this.getClass(), uriInfo);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        LOGGER.trace("GET: Using auto hierarchy path {} to retrieve resource.", jcrPath);
        final FedoraResource resource = nodeService.getObject(session, jcrPath);

        checkCacheControlHeaders(request, servletResponse, resource);

        final RdfStream rdfStream =
            resource.getTriples(subjects).session(session)
                    .topic(subjects.getSubject(resource.getNode().getPath())
                            .asNode());

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else {
            returnPreference = new PreferTag("");
        }

        if (!returnPreference.getValue().equals("minimal")) {
            String include = returnPreference.getParams().get("include");
            if (include == null) {
                include = "";
            }

            String omit = returnPreference.getParams().get("omit");
            if (omit == null) {
                omit = "";
            }

            final String[] includes = include.split(" ");
            final String[] omits = omit.split(" ");

            if (limit >= 0) {
                final Node firstPage =
                    createURI(uriInfo.getRequestUriBuilder().replaceQueryParam("offset", 0)
                                  .replaceQueryParam("limit", limit).build()
                                  .toString().replace("&", "&amp;"));
                rdfStream.concat(create(subjects.getContext().asNode(), FIRST_PAGE.asNode(), firstPage));
                servletResponse.addHeader("Link", "<" + firstPage + ">;rel=\"first\"");

                if ( resource.getNode().getNodes().getSize() > (offset + limit) ) {
                    final Node nextPage =
                        createURI(uriInfo.getRequestUriBuilder().replaceQueryParam("offset", offset + limit)
                                  .replaceQueryParam("limit", limit).build()
                                  .toString().replace("&", "&amp;"));
                    rdfStream.concat(create(subjects.getContext().asNode(), NEXT_PAGE.asNode(), nextPage));
                    servletResponse.addHeader("Link", "<" + nextPage + ">;rel=\"next\"");
                }
            }

            List<String> appliedIncludes = new ArrayList<>();

            final boolean membership =
                (!contains(includes, LDP_NAMESPACE + "PreferEmptyContainer") ||
                     contains(includes, LDP_NAMESPACE + "PreferMembership"))
                    && !contains(omits, LDP_NAMESPACE + "PreferMembership");

            final boolean containment =
                (!contains(includes, LDP_NAMESPACE + "PreferEmptyContainer") ||
                     contains(includes, LDP_NAMESPACE + "PreferContainment"))
                    && !contains(omits, LDP_NAMESPACE + "PreferContainment");


            final boolean references = contains(includes, INBOUND_REFERENCES.toString())
                                           && !contains(omits, INBOUND_REFERENCES.toString());

            final HierarchyRdfContextOptions hierarchyRdfContextOptions
                = new HierarchyRdfContextOptions(limit, offset, membership, containment);

            if (hierarchyRdfContextOptions.membershipEnabled()) {
                appliedIncludes.add(LDP_NAMESPACE + "PreferMembership");
            }

            if (hierarchyRdfContextOptions.containmentEnabled()) {
                appliedIncludes.add(LDP_NAMESPACE + "PreferContainment");
            }

            if (references) {
                rdfStream.concat(resource.getReferencesTriples(subjects));
                appliedIncludes.add(INBOUND_REFERENCES.toString());
            }

            rdfStream.concat(resource.getHierarchyTriples(subjects, hierarchyRdfContextOptions));

            final String preferences = "return=representation; include=\""
                                           + Iterators.toString(appliedIncludes.iterator(), " ") + "\"";
            servletResponse.addHeader("Preference-Applied", preferences);

        } else {
            servletResponse.addHeader("Preference-Applied", "return=minimal");
        }
        servletResponse.addHeader("Vary", "Prefer");

        addResourceHttpHeaders(servletResponse, resource, subjects);

        addResponseInformationToStream(resource, rdfStream, uriInfo,
                subjects);

        return rdfStream;


    }

    private void addResourceHttpHeaders(final HttpServletResponse servletResponse,
                                        final FedoraResource resource,
                                        final HttpIdentifierTranslator subjects) throws RepositoryException {

        if (resource.hasContent()) {
            servletResponse.addHeader("Link", "<" + subjects.getSubject(
                resource.getNode().getNode(JCR_CONTENT).getPath()) + ">;rel=\"describes\"");
        }

        if (!subjects.isCanonical()) {
            final IdentifierTranslator subjectsCanonical = subjects.getCanonical(true);

            servletResponse.addHeader("Link",
                "<" + subjectsCanonical.getSubject(resource.getPath()) + ">;rel=\"canonical\"");
        }

        addOptionsHttpHeaders(servletResponse);
        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");
        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\"");
    }

    private void addOptionsHttpHeaders(final HttpServletResponse servletResponse) {
        servletResponse.addHeader("Accept-Patch", contentTypeSPARQLUpdate);

        servletResponse.addHeader("Allow", "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS");
        final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT1 + ","
                                    + N3_ALT2 + "," + RDF_XML + "," + NTRIPLES;
        servletResponse.addHeader("Accept-Post", rdfTypes + "," + MediaType.MULTIPART_FORM_DATA
                                                     + "," + contentTypeSPARQLUpdate);
    }

    /**
     * Update an object using SPARQL-UPDATE
     *
     * @param pathList
     * @return 201
     * @throws RepositoryException
     * @throws org.fcrepo.kernel.exception.InvalidChecksumException
     * @throws IOException
     */
    @PATCH
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@PathParam("path")
            final List<PathSegment> pathList,
            @Context
            final UriInfo uriInfo,
            final InputStream requestBodyStream,
            @Context final Request request, @Context final HttpServletResponse servletResponse)
        throws RepositoryException, IOException {

        final String path = toPath(pathList);
        LOGGER.debug("Attempting to update path: {}", path);

        try {
            final IdentifierTranslator subjects = new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.trace("PATCH: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            LOGGER.trace("Converted incoming path {} to path: {}", path, jcrPath);
            if (requestBodyStream != null) {

                final FedoraResource resource =
                        nodeService.getObject(session, jcrPath);

                evaluateRequestPreconditions(request, resource);

                final Dataset properties = resource.updatePropertiesDataset(subjects, IOUtils
                        .toString(requestBodyStream));

                final Model problems = properties.getNamedModel(PROBLEMS_MODEL_NAME);
                if (!problems.isEmpty()) {
                    LOGGER.info(
                                   "Found these problems updating the properties for {}: {}",
                                   path, problems);
                    return status(FORBIDDEN).entity(problems.toString())
                            .build();

                }

                session.save();
                versionService.nodeUpdated(resource.getNode());

                addCacheControlHeaders(servletResponse, resource);

                return noContent().build();
            }
            return status(SC_BAD_REQUEST).entity(
                    "SPARQL-UPDATE requests must have content!").build();

        } finally {
            session.logout();
        }
    }

    /**
     * Replace triples with triples from a new model
     * @param pathList
     * @param uriInfo
     * @param requestContentType
     * @param requestBodyStream
     * @return
     * @throws Exception
     */
    @PUT
    @Consumes({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, NTRIPLES})
    @Timed
    public Response createOrReplaceObjectRdf(
            @PathParam("path") final List<PathSegment> pathList,
            @Context final UriInfo uriInfo,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            final InputStream requestBodyStream,
            @Context
            final Request request,
            @Context final HttpServletResponse servletResponse) throws RepositoryException, ParseException,
            IOException, InvalidChecksumException, URISyntaxException {
        final String path = toPath(pathList);
        LOGGER.debug("Attempting to replace path: {}", path);
        try {

            final FedoraResource resource;
            final Response.ResponseBuilder response;


            final MediaType contentType = getSimpleContentType(requestContentType);
            final HttpIdentifierTranslator idTranslator =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            final String jcrPath = idTranslator.getPathFromSubject(createResource(uriInfo.getBaseUri() + path));
            LOGGER.trace("PUT: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            if (nodeService.exists(session, jcrPath)) {
                resource = nodeService.getObject(session, jcrPath);
                response = noContent();
            } else {
                final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
                resource = createFedoraResource(null, effectiveContentType, jcrPath);
                final URI location = new URI(idTranslator.getSubject(resource.getNode().getPath()).getURI());

                response = created(location).entity(location.toString());
            }

            evaluateRequestPreconditions(request, resource);

            if (requestContentType != null && requestBodyStream != null)  {
                final String format = contentTypeToLang(contentType.toString()).getName().toUpperCase();

                final Model inputModel = createDefaultModel()
                                             .read(requestBodyStream,
                                                     idTranslator.getSubject(resource.getNode().getPath()).toString(),
                                                      format);

                resource.replaceProperties(idTranslator, inputModel);

            }

            session.save();
            addCacheControlHeaders(servletResponse, resource);
            versionService.nodeUpdated(resource.getNode());

            return response.build();
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new object.
     *
     * @param pathList
     * @return 201
     * @throws Exception
     */
    @POST
    @Timed
    public Response createObject(@PathParam("path")
            final List<PathSegment> pathList,
            @QueryParam("mixin")
            final String mixin,
            @QueryParam("checksum")
            final String checksum,
            @HeaderParam("Content-Disposition") final String contentDisposition,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            @HeaderParam("Slug")
            final String slug,
            @Context final HttpServletResponse servletResponse,
            @Context
            final UriInfo uriInfo, final InputStream requestBodyStream)
        throws RepositoryException, ParseException, IOException,
                   InvalidChecksumException, URISyntaxException {
        String pid;
        final String newObjectPath;
        final String path = toPath(pathList);
        LOGGER.trace("POST: Attempting to created object in path: {}", path);
        final HttpIdentifierTranslator idTranslator =
            new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

        final MediaType contentType = getSimpleContentType(requestContentType);

        final String contentTypeString = contentType.toString();

        final String jcrPath = idTranslator.getPathFromSubject(createResource(uriInfo.getBaseUri() + path));
        assertPathExists(jcrPath);

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else {
            pid = pidMinter.mintPid();
        }
        // reverse translate the proffered or created identifier
        LOGGER.trace("Using external identifier {} to create new resource.", pid);
        LOGGER.trace("Using prefixed external identifier {} to create new resource.", uriInfo.getBaseUri() + "/"
                                                                                          + pid);
        newObjectPath = idTranslator.getPathFromSubject(createResource(uriInfo.getBaseUri() +
                                                                       path + (path.equals("/") ? "" : "/") + pid));
        LOGGER.trace("Using auto hierarchy path {} to create new resource.", newObjectPath);
        // remove leading slash left over from translation
        //pid = pid.substring(1, pid.length());

        //newObjectPath = path + "/" + pid;

        assertPathMissing(newObjectPath);

        final FedoraResource object = nodeService.getObject(session,
                idTranslator.getPathFromSubject(createResource(uriInfo.getBaseUri() + path)));

        if (object.getModels().contains(FEDORA_DATASTREAM)) {
            throw new WebApplicationException(conflict().entity("Object cannot have child nodes").build());
        }

        LOGGER.debug("Attempting to ingest with path: {}", newObjectPath);

        try {

            final MediaType effectiveContentType
                = requestBodyStream == null || requestContentType == null ? null : contentType;
            final FedoraResource result = createFedoraResource(mixin,
                                                                  effectiveContentType,
                                                                  newObjectPath);

            final Response.ResponseBuilder response;
            final URI location = new URI(idTranslator.getSubject(result.getNode().getPath()).getURI());

            if (requestBodyStream == null || requestContentType == null) {
                LOGGER.trace("No request body detected");
                response = created(location).entity(location.toString());
            } else {
                LOGGER.trace("Received createObject with a request body and content type \"{}\"", contentTypeString);

                if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                    LOGGER.trace("Found SPARQL-Update content, applying..");
                    result.updatePropertiesDataset(idTranslator, IOUtils.toString(requestBodyStream));
                    if (result.isNew()) {
                        response = created(location).entity(location.toString());
                    } else {
                        response = noContent();
                    }
                } else  if (isRdfContentType(contentTypeString)) {
                    LOGGER.trace("Found a RDF syntax, attempting to replace triples");

                    final Lang lang = contentTypeToLang(contentTypeString);

                    final String format = lang.getName().toUpperCase();

                    final Model inputModel =
                        createDefaultModel().read(requestBodyStream,
                                idTranslator.getSubject(result.getNode().getPath()).toString(), format);

                    result.replaceProperties(idTranslator, inputModel);
                    response = created(location).entity(location.toString());
                } else if (result instanceof Datastream) {
                    LOGGER.trace("Created a datastream and have a binary payload.");

                    final URI checksumURI;

                    if (checksum != null && !checksum.equals("")) {
                        checksumURI = new URI(checksum);
                    } else {
                        checksumURI = null;
                    }


                    final String originalFileName;

                    if (contentDisposition != null) {
                        final ContentDisposition disposition = new ContentDisposition(contentDisposition);
                        originalFileName = disposition.getFileName();
                    } else {
                        originalFileName = null;
                    }

                    datastreamService.createDatastream(session,
                            newObjectPath, contentTypeString, originalFileName, requestBodyStream, checksumURI);
                    final URI contentLocation =
                        new URI(idTranslator.getSubject(((Datastream) result).getContentNode().getPath()).getURI());
                    response = created(contentLocation).entity(contentLocation.toString());

                } else {
                    if (requestBodyStream.read() != -1) {
                        LOGGER.trace("Unknown content type: {}", contentType);
                        response = notAcceptable().entity("Invalid Content type " + contentType);
                        throw new WebApplicationException(response.build());
                    } else {
                        response = created(location).entity(location.toString());
                    }
                }
            }

            session.save();
            versionService.nodeUpdated(result.getNode());

            LOGGER.debug("Finished creating {} with path: {}", mixin, newObjectPath);

            addCacheControlHeaders(servletResponse, result);

            return response.build();

        } finally {
            session.logout();
        }
    }

    private FedoraResource createFedoraResource(final String requestMixin,
                                                final MediaType requestContentType,
                                                final String path) throws RepositoryException {
        final String objectType = getRequestedObjectType(requestMixin, requestContentType);

        final FedoraResource result;

        switch (objectType) {
            case FEDORA_OBJECT:
                result = objectService.createObject(session, path);
                break;
            case FEDORA_DATASTREAM:
                result = datastreamService.createDatastream(session, path);
                break;
            default:
                throw new WebApplicationException(clientError().entity(
                        "Unknown object type " + objectType).build());
        }
        return result;
    }

    private void assertPathMissing(final String path) throws RepositoryException {
        if (nodeService.exists(session, path)) {
            throw new WebApplicationException(conflict().entity(path + " is an existing resource!").build());
        }
    }

    private void assertPathExists(final String path) throws RepositoryException {
        if (!nodeService.exists(session, path)) {
            throw new WebApplicationException(notFound().build());
        }
    }

    private String getRequestedObjectType(final String mixin, final MediaType requestContentType) {
        String objectType = FEDORA_OBJECT;

        if (mixin != null) {
            objectType = mixin;
        } else {
            if (requestContentType != null) {
                final String s = requestContentType.toString();
                if (!s.equals(contentTypeSPARQLUpdate) && !isRdfContentType(s)) {
                    objectType = FEDORA_DATASTREAM;
                }
            }
        }
        return objectType;
    }

    /**
     * Create a new object from a multipart/form-data POST request
     * @param pathList
     * @param mixin
     * @param slug
     * @param uriInfo
     * @param file
     * @return
     * @throws Exception
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Timed
    public Response createObjectFromFormPost(
                                                @PathParam("path") final List<PathSegment> pathList,
                                                @FormDataParam("mixin") final String mixin,
                                                @FormDataParam("slug") final String slug,
                                                @Context final HttpServletResponse servletResponse,
                                                @Context final UriInfo uriInfo,
                                                @FormDataParam("file") final InputStream file
    ) throws RepositoryException, URISyntaxException, InvalidChecksumException, ParseException, IOException {

        final MediaType effectiveContentType = file == null ? null : MediaType.APPLICATION_OCTET_STREAM_TYPE;
        return createObject(pathList, mixin, null, null, effectiveContentType, slug, servletResponse, uriInfo, file);

    }

    /**
     * Deletes an object.
     *
     * @param pathList
     * @return
     * @throws RepositoryException
     */
    @DELETE
    @Timed
    public Response deleteObject(@PathParam("path")
            final List<PathSegment> pathList,
            @Context final Request request) throws RepositoryException {

        try {
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, this.getClass(), uriInfo);

            final String path = toPath(pathList);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.trace("DELETE: Using auto hierarchy path {} to retrieve resource.", jcrPath);
            final FedoraResource resource =
                nodeService.getObject(session, jcrPath);
            evaluateRequestPreconditions(request, resource);

            nodeService.deleteObject(session, jcrPath);
            session.save();
            return noContent().build();
        } catch (javax.jcr.ReferentialIntegrityException riex) {
            StringBuffer msg = new StringBuffer("Unable to delete node because it is linked to "
                    + "by other nodes: ");

            // lookup paths of linking nodes
            Throwable inner = riex.getCause();
            if ( inner instanceof ReferentialIntegrityException) {
                for ( NodeKey node : ((ReferentialIntegrityException)inner).getReferrers() ) {
                    try {
                        msg.append( " " + session.getNodeByIdentifier(node.getIdentifier()).getPath() );
                    } catch ( Exception ex ) {
                        msg.append( " <" + node.getIdentifier() + ">");
                    }
                }
            }
            return status(SC_PRECONDITION_FAILED).entity(msg.toString()).build();
        } catch (WebApplicationException ex) {
            return (Response)ex.getResponse();
        } finally {
            session.logout();
        }
    }

    /**
     * Copies an object from one path to another
     */
    @COPY
    @Timed
    public Response copyObject(@PathParam("path") final List<PathSegment> path,
                               @HeaderParam("Destination") final String destinationUri)
        throws RepositoryException, URISyntaxException {

        try {

            final IdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
            String srcPath = subjects.getPathFromSubject(createResource(uriInfo.getBaseUri() + toPath(path)));
            LOGGER.trace("COPY: Using auto hierarchy path {} to retrieve source.", srcPath);
            if (!nodeService.exists(session, srcPath)) {
                return status(SC_CONFLICT).entity("The source path does not exist").build();
            }

            final String destination =
                subjects.getPathFromSubject(ResourceFactory.createResource(destinationUri));
            LOGGER.trace("COPY: Using auto hierarchy path {} to retrieve destination resource.", destination);
            if (destination == null) {
                return status(SC_BAD_GATEWAY).entity("Destination was not a valid resource path").build();
            }

            nodeService.copyObject(session, srcPath, destination);
            session.save();
            versionService.nodeUpdated(session, destination);
            return created(new URI(destinationUri)).build();
        } catch (final ItemExistsException e) {
            throw new WebApplicationException(e,
                status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build());
        } catch (final PathNotFoundException e) {
            throw new WebApplicationException(e, status(SC_CONFLICT).entity(
                    "There is no node that will serve as the parent of the moved item")
                    .build());
        } finally {
            session.logout();
        }

    }

    /**
     * Copies an object from one path to another
     */
    @MOVE
    @Timed
    public Response moveObject(@PathParam("path") final List<PathSegment> pathList,
                               @HeaderParam("Destination") final String destinationUri,
                               @Context final Request request)
        throws RepositoryException, URISyntaxException {
        final IdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);
        try {

            final String path = toPath(pathList);
            String srcPath = subjects.getPathFromSubject(createResource(uriInfo.getBaseUri() + path));
            LOGGER.trace("MOVE: Using auto hierarchy path {} to retrieve source resource.", srcPath);
            if (!nodeService.exists(session, srcPath)) {
                return status(SC_CONFLICT).entity("The source path does not exist").build();
            }


            final FedoraResource resource =
                nodeService.getObject(session, srcPath);


            evaluateRequestPreconditions(request, resource);

            final String destination =
                subjects.getPathFromSubject(ResourceFactory.createResource(destinationUri));
            LOGGER.trace("MOVE: Using auto hierarchy path {} to retrieve destination resource.", destination);
            if (destination == null) {
                return status(SC_BAD_GATEWAY).entity("Destination was not a valid resource path").build();
            }

            nodeService.moveObject(session, path, destination);
            session.save();
            versionService.nodeUpdated(session, destination);
            return created(new URI(destinationUri)).build();
        } catch (final ItemExistsException e) {
            throw new WebApplicationException(e,
                status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build());
        } catch (final PathNotFoundException e) {
            throw new WebApplicationException(e, status(SC_CONFLICT).entity(
                    "There is no node that will serve as the parent of the moved item")
                    .build());
        } finally {
            session.logout();
        }

    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     */
    @OPTIONS
    @Timed
    public Response options(@PathParam("path") final List<PathSegment> pathList,
                            @Context final HttpServletResponse servletResponse)
        throws RepositoryException {
        addOptionsHttpHeaders(servletResponse);
        return status(OK).build();
    }

}
