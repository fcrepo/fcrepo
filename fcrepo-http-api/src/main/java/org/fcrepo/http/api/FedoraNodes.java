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
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
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
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.COPY;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.domain.MOVE;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.sun.jersey.multipart.FormDataParam;

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
    private static boolean baseURLSet = false;

    /**
     * Set the baseURL for JMS events.
    **/
    private void init( final UriInfo uriInfo ) {
        if ( !baseURLSet ) {
            // set to true the first time this is run.  if there is an exception the first time, there
            // will likely be an exception every time.  since this is run on each repository update,
            // we should fail fast rather than retrying over and over.
            baseURLSet = true;
            try {
                final URI baseURL = uriInfo.getBaseUri();
                LOGGER.debug("FedoraNodes.init(): baseURL = " + baseURL.toString());
                final ObservationManager obs = session.getWorkspace().getObservationManager();
                final String json = "{\"baseURL\":\"" + baseURL.toString() + "\"}";
                obs.setUserData(json);
                LOGGER.trace("FedoraNodes.init(): done");
            } catch ( Exception ex ) {
                LOGGER.warn("Error setting baseURL", ex);
            }
        }
    }

    /**
     * Retrieve the node headers
     * @param pathList
     * @param request
     * @param servletResponse
     * @param uriInfo
     * @return response
     * @throws RepositoryException
     */
    @HEAD
    @Timed
    public Response head(@PathParam("path") final List<PathSegment> pathList,
                     @Context final Request request,
                     @Context final HttpServletResponse servletResponse,
                     @Context final UriInfo uriInfo) {
        throwIfPathIncludesJcr(pathList, "HEAD");

        final String path = toPath(pathList);
        LOGGER.trace("Getting head for: {}", path);

        final FedoraResource resource = nodeService.getObject(session, path);

        final HttpIdentifierTranslator subjects =
            new HttpIdentifierTranslator(session, this.getClass(), uriInfo);

        checkCacheControlHeaders(request, servletResponse, resource, session);

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
     * @return triples for the specified node
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML, JSON_LD})
    public RdfStream describe(@PathParam("path") final List<PathSegment> pathList,
            @QueryParam("offset") @DefaultValue("0") final int offset,
            @QueryParam("limit")  @DefaultValue("-1") final int limit,
            @HeaderParam("Prefer") final Prefer prefer,
            @Context final Request request,
            @Context final HttpServletResponse servletResponse,
            @Context final UriInfo uriInfo) {
        throwIfPathIncludesJcr(pathList, "MOVE");

        final String path = toPath(pathList);
        LOGGER.trace("Getting profile for: {}", path);

        final FedoraResource resource = nodeService.getObject(session, path);

        checkCacheControlHeaders(request, servletResponse, resource, session);

        final HttpIdentifierTranslator subjects =
            new HttpIdentifierTranslator(session, this.getClass(), uriInfo);

        final RdfStream rdfStream =
            resource.getTriples(subjects, PropertiesRdfContext.class).session(session)
                    .topic(subjects.getSubject(resource.getPath()).asNode());

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
                try {
                    final Node firstPage =
                            createURI(uriInfo.getRequestUriBuilder().replaceQueryParam("offset", 0)
                                    .replaceQueryParam("limit", limit).build()
                                    .toString().replace("&", "&amp;"));
                    rdfStream.concat(create(subjects.getContext().asNode(), FIRST_PAGE.asNode(), firstPage));
                    servletResponse.addHeader("Link", "<" + firstPage + ">;rel=\"first\"");

                    if (resource.getNode().getNodes().getSize() > (offset + limit)) {
                        final Node nextPage =
                                createURI(uriInfo.getRequestUriBuilder().replaceQueryParam("offset", offset + limit)
                                        .replaceQueryParam("limit", limit).build()
                                        .toString().replace("&", "&amp;"));
                        rdfStream.concat(create(subjects.getContext().asNode(), NEXT_PAGE.asNode(), nextPage));
                        servletResponse.addHeader("Link", "<" + nextPage + ">;rel=\"next\"");
                    }
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }

            final boolean membership =
                (!contains(includes, LDP_NAMESPACE + "PreferMinimalContainer") ||
                     contains(includes, LDP_NAMESPACE + "PreferMembership"))
                    && !contains(omits, LDP_NAMESPACE + "PreferMembership");

            final boolean containment =
                (!contains(includes, LDP_NAMESPACE + "PreferMinimalContainer") ||
                     contains(includes, LDP_NAMESPACE + "PreferContainment"))
                    && !contains(omits, LDP_NAMESPACE + "PreferContainment");


            final boolean references = !contains(omits, INBOUND_REFERENCES.toString());

            final HierarchyRdfContextOptions hierarchyRdfContextOptions
                = new HierarchyRdfContextOptions(limit, offset, membership, containment);

            if (references) {
                rdfStream.concat(resource.getTriples(subjects, ReferencesRdfContext.class));
            }

            rdfStream.concat(resource.getHierarchyTriples(subjects, hierarchyRdfContextOptions));

            servletResponse.addHeader("Preference-Applied", "return=representation");

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
                                        final HttpIdentifierTranslator subjects) {

        if (resource.hasContent()) {
            try {
                servletResponse.addHeader("Link", "<" + subjects.getSubject(
                        resource.getNode().getNode(JCR_CONTENT).getPath()) + ">;rel=\"describes\"");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }

        if (!subjects.isCanonical()) {
            final IdentifierTranslator subjectsCanonical = subjects.getCanonical(true);

            try {
                servletResponse.addHeader("Link",
                        "<" + subjectsCanonical.getSubject(resource.getPath()) + ">;rel=\"canonical\"");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
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
     * @throws IOException
     */
    @PATCH
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@PathParam("path")
            final List<PathSegment> pathList,
            @Context
            final UriInfo uriInfo,
            @ContentLocation final InputStream requestBodyStream,
            @Context final Request request, @Context final HttpServletResponse servletResponse)
        throws IOException {
        throwIfPathIncludesJcr(pathList, "PATCH");

        init(uriInfo);
        final String path = toPath(pathList);
        LOGGER.debug("Attempting to update path: {}", path);

        if (null == requestBodyStream) {
            return status(SC_BAD_REQUEST).entity("SPARQL-UPDATE requests must have content!").build();
        }

        try {
            final String requestBody = IOUtils.toString(requestBodyStream);
            if (isBlank(requestBody)) {
                return status(SC_BAD_REQUEST).entity("SPARQL-UPDATE requests must have content!").build();
            }

            final FedoraResource resource =
                    nodeService.getObject(session, path);

            evaluateRequestPreconditions(request, servletResponse, resource, session);

            final Dataset properties = resource.updatePropertiesDataset(new HttpIdentifierTranslator(
                    session, FedoraNodes.class, uriInfo), requestBody);

            final Model problems = properties.getNamedModel(PROBLEMS_MODEL_NAME);
            if (!problems.isEmpty()) {
                LOGGER.info(
                        "Found these problems updating the properties for {}: {}",
                        path, problems);
                final StringBuilder error = new StringBuilder();
                final StmtIterator sit = problems.listStatements();
                while (sit.hasNext()) {
                    final String message = getMessage(sit.next());
                    if (StringUtils.isNotEmpty(message) && error.indexOf(message) < 0) {
                        error.append(message + " \n");
                    }
                }
                return status(FORBIDDEN).entity(error.length() > 0 ? error.toString() : problems.toString())
                        .build();
            }

            try {
                session.save();
                versionService.nodeUpdated(resource.getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, resource, session);

            return noContent().build();

        } catch ( final RuntimeException ex ) {
            final Throwable cause = ex.getCause();
            if ( cause != null && cause instanceof PathNotFoundException ) {
                // the sparql update referred to a repository resource that doesn't exist
                return status(SC_BAD_REQUEST).entity(cause.getMessage()).build();
            }
            throw ex;
        } finally {
            session.logout();
        }
    }

    /**
     * Create a resource at a specified path, or replace triples with provided RDF.
     * @param pathList
     * @param uriInfo
     * @param requestContentType
     * @param requestBodyStream
     * @return 204
     */
    @PUT
    @Consumes({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, NTRIPLES, JSON_LD})
    @Timed
    public Response createOrReplaceObjectRdf(
            @PathParam("path") final List<PathSegment> pathList,
            @Context final UriInfo uriInfo,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            @ContentLocation final InputStream requestBodyStream,
            @Context final Request request,
            @Context final HttpServletResponse servletResponse) throws URISyntaxException {
        throwIfPathIncludesJcr(pathList, "PUT");
        init(uriInfo);

        final String path = toPath(pathList);
        LOGGER.debug("Attempting to replace path: {}", path);
        try {

            final FedoraResource resource;
            final Response.ResponseBuilder response;


            final MediaType contentType = getSimpleContentType(requestContentType);

            final boolean preexisting;
            if (nodeService.exists(session, path)) {
                resource = nodeService.getObject(session, path);
                response = noContent();
                preexisting = true;
            } else {
                final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
                resource = createFedoraResource(null, effectiveContentType, path);
                final HttpIdentifierTranslator idTranslator =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

                final URI location = new URI(idTranslator.getSubject(resource.getPath()).getURI());

                response = created(location).entity(location.toString());
                preexisting = false;
            }

            evaluateRequestPreconditions(request, servletResponse, resource, session);

            final HttpIdentifierTranslator graphSubjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            if (requestContentType != null && requestBodyStream != null)  {
                final String format = contentTypeToLang(contentType.toString()).getName().toUpperCase();

                final Model inputModel = createDefaultModel()
                                             .read(requestBodyStream,
                                                      graphSubjects.getSubject(resource.getPath()).toString(),
                                                      format);

                resource.replaceProperties(graphSubjects, inputModel, resource.getTriples(graphSubjects, PropertiesRdfContext.class));

            } else if (preexisting) {
                return status(SC_CONFLICT).entity("No RDF provided and the resource already exists!").build();
            }

            try {
                session.save();
                versionService.nodeUpdated(resource.getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, resource, session);

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
            final UriInfo uriInfo,
            @ContentLocation final InputStream requestBodyStream)
        throws ParseException, IOException,
                   InvalidChecksumException, URISyntaxException {
        throwIfPathIncludesJcr(pathList, "POST");
        init(uriInfo);

        String pid;
        final String newObjectPath;
        final String path = toPath(pathList);

        final HttpIdentifierTranslator idTranslator =
            new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

        final MediaType contentType = getSimpleContentType(requestContentType);

        final String contentTypeString = contentType.toString();

        assertPathExists(path);

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else {
            pid = pidMinter.mintPid();
        }
        // reverse translate the proffered or created identifier
        LOGGER.trace("Using external identifier {} to create new resource.", pid);
        LOGGER.trace("Using prefixed external identifier {} to create new resource.", uriInfo.getBaseUri() + "/"
                                                                                          + pid);
        pid = idTranslator.getPathFromSubject(createResource(uriInfo.getBaseUri() + "/" + pid));
        // remove leading slash left over from translation
        pid = pid.substring(1, pid.length());
        LOGGER.trace("Using internal identifier {} to create new resource.", pid);
        newObjectPath = path + "/" + pid;

        assertPathMissing(newObjectPath);

        final FedoraResource object = nodeService.getObject(session, path);

        if (object.hasType(FEDORA_DATASTREAM)) {
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
            final URI location = new URI(idTranslator.getSubject(result.getPath()).getURI());

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
                } else if (isRdfContentType(contentTypeString)) {
                    LOGGER.trace("Found a RDF syntax, attempting to replace triples");

                    final Lang lang = contentTypeToLang(contentTypeString);

                    final String format = lang.getName().toUpperCase();

                    final Model inputModel =
                        createDefaultModel().read(requestBodyStream,
                                idTranslator.getSubject(result.getPath()).toString(), format);

                    result.replaceProperties(idTranslator, inputModel, result.getTriples(idTranslator, PropertiesRdfContext.class));
                    response = created(location).entity(location.toString());
                } else if (result instanceof Datastream) {
                    LOGGER.trace("Created a datastream and have a binary payload.");

                    final URI checksumURI = checksumURI(checksum);
                    final String originalFileName = originalFileName(contentDisposition);

                    datastreamService.createDatastream(session,
                            newObjectPath, contentTypeString, originalFileName, requestBodyStream, checksumURI);
                    final URI contentLocation;

                    try {
                        contentLocation = new URI(
                                idTranslator.getSubject(((Datastream) result).getContentNode().getPath()).getURI()
                        );
                    } catch (final RepositoryException e) {
                        throw new RepositoryRuntimeException(e);
                    }

                    response = created(contentLocation).entity(contentLocation.toString());

                } else {
                    response = created(location).entity(location.toString());
                }
            }

            try {
                session.save();
                versionService.nodeUpdated(result.getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            LOGGER.debug("Finished creating {} with path: {}", mixin, newObjectPath);

            addCacheControlHeaders(servletResponse, result, session);

            return response.build();

        } finally {
            session.logout();
        }
    }

    private FedoraResource createFedoraResource(final String requestMixin,
                                                final MediaType requestContentType,
                                                final String path) {
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

    private void assertPathMissing(final String path) {
        if (nodeService.exists(session, path)) {
            throw new WebApplicationException(conflict().entity(path + " is an existing resource!").build());
        }
    }

    private void assertPathExists(final String path) {
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
     * @return response
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
    ) throws URISyntaxException, InvalidChecksumException, ParseException, IOException {
        throwIfPathIncludesJcr(pathList, "POST with multipart attachment");
        init(uriInfo);

        final MediaType effectiveContentType = file == null ? null : MediaType.APPLICATION_OCTET_STREAM_TYPE;
        return createObject(pathList, mixin, null, null, effectiveContentType, slug, servletResponse, uriInfo, file);

    }

    /**
     * Deletes an object.
     *
     * @param pathList
     * @return response
     * @throws RepositoryException
     */
    @DELETE
    @Timed
    public Response deleteObject(@PathParam("path") final List<PathSegment> pathList,
                                 @Context final Request request,
                                 @Context final HttpServletResponse servletResponse) {
        throwIfPathIncludesJcr(pathList, "DELETE");
        init(uriInfo);

        try {

            final String path = toPath(pathList);

            final FedoraResource resource =
                nodeService.getObject(session, path);
            evaluateRequestPreconditions(request, servletResponse, resource, session);

            nodeService.deleteObject(session, path);

            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            return noContent().build();
        } catch (final WebApplicationException ex) {
            return ex.getResponse();
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
        throws URISyntaxException {
        throwIfPathIncludesJcr(path, "COPY");
        init(uriInfo);

        try {

            final IdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            if (!nodeService.exists(session, toPath(path))) {
                return status(SC_CONFLICT).entity("The source path does not exist").build();
            }

            final String destination =
                subjects.getPathFromSubject(ResourceFactory.createResource(destinationUri));

            if (destination == null) {
                return status(SC_BAD_GATEWAY).entity("Destination was not a valid resource path").build();
            } else if (nodeService.exists(session, destination)) {
                return status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build();
            }


            nodeService.copyObject(session, toPath(path), destination);

            session.save();
            versionService.nodeUpdated(session, destination);

            return created(new URI(destinationUri)).build();
        } catch (final RepositoryRuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof ItemExistsException) {

                throw new WebApplicationException(e,
                        status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build());

            } else if (cause instanceof PathNotFoundException) {

                throw new WebApplicationException(e, status(SC_CONFLICT).entity(
                        "There is no node that will serve as the parent of the copied item")
                        .build());
            } else {
                throw e;
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
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
                               @Context final Request request,
                               @Context final HttpServletResponse servletResponse)
        throws URISyntaxException {
        throwIfPathIncludesJcr(pathList, "MOVE");
        init(uriInfo);

        try {

            final String path = toPath(pathList);

            if (!nodeService.exists(session, path)) {
                return status(SC_CONFLICT).entity("The source path does not exist").build();
            }


            final FedoraResource resource =
                nodeService.getObject(session, path);


            evaluateRequestPreconditions(request, servletResponse, resource, session);

            final IdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            final String destination =
                subjects.getPathFromSubject(ResourceFactory.createResource(destinationUri));

            if (destination == null) {
                return status(SC_BAD_GATEWAY).entity("Destination was not a valid resource path").build();
            } else if (nodeService.exists(session, destination)) {
                return status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build();
            }

            nodeService.moveObject(session, path, destination);
            session.save();
            versionService.nodeUpdated(session, destination);
            return created(new URI(destinationUri)).build();
        } catch (final RepositoryRuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof ItemExistsException) {

                throw new WebApplicationException(e,
                        status(SC_PRECONDITION_FAILED).entity("Destination resource already exists").build());

            } else if (cause instanceof PathNotFoundException) {

                throw new WebApplicationException(e, status(SC_CONFLICT).entity(
                        "There is no node that will serve as the parent of the moved item")
                        .build());
            } else {
                throw e;
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
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
                            @Context final HttpServletResponse servletResponse) {
        throwIfPathIncludesJcr(pathList, "OPTIONS");

        addOptionsHttpHeaders(servletResponse);
        return status(OK).build();
    }

    /**
     * Method to check for any jcr namespace element in the path
     */
    @VisibleForTesting
    protected void throwIfPathIncludesJcr(final List<PathSegment> pathList, final String msg) {
        if (pathList == null || pathList.size() == 0) {
            return;
        }
        final PathSegment pathSegment = pathList.get(pathList.size() - 1);
        final String[] tokens = pathSegment.getPath().split(":");
        if (tokens.length == 2 && tokens[0].equalsIgnoreCase("jcr")) {
            final String requestPath = uriInfo.getPath();
            LOGGER.trace("{} request with jcr namespace is not allowed: {} ", msg, requestPath);
            throw new WebApplicationException(notFound().build());
        }
    }

    /*
     * Return the statement's predicate and its literal value if there's any
     * @param stmt
     * @return
     */
    private static String getMessage(final Statement stmt) {
        final Literal literal = stmt.getLiteral();
        if (literal != null) {
            return stmt.getPredicate().getURI() + ": " + literal.getString();
        }
        return null;
    }
}
