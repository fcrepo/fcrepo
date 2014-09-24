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

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang.StringUtils.isBlank;
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
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Function;
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
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * CRUD operations on Fedora Nodes
 *
 * @author cbeer
 */
@Scope("request")
@Path("/{path: .*}")
public class FedoraNodes extends FedoraLdp {

    private static final Logger LOGGER = getLogger(FedoraNodes.class);
    private static boolean baseURLSet = false;

    @PathParam("path") protected List<PathSegment> pathList;

    @PostConstruct
    private void postConstruct() {
        throwIfPathIncludesJcr(pathList);
        this.path = toPath(pathList);
        init(uriInfo);
    }

    /**
     * Default JAX-RS entry point
     */
    @Inject
    public FedoraNodes() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param path
     */
    @VisibleForTesting
    public FedoraNodes(final String path) {
        super(path);
    }

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
     * Retrieve the node profile
     *
     * @return triples for the specified node
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE + ";qs=10", JSON_LD + ";qs=8",
            N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream describe(@HeaderParam("Prefer") final Prefer prefer) {
        LOGGER.trace("Getting profile for: {}", path);

        checkCacheControlHeaders(request, servletResponse, resource(), session);

        final RdfStream rdfStream = getTriples(PropertiesRdfContext.class).session(session)
                .topic(translator().getSubject(resource().getPath()).asNode());

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else {
            returnPreference = new PreferTag("");
        }

        if (!returnPreference.getValue().equals("minimal")) {
            final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

            if (ldpPreferences.prefersReferences()) {
                rdfStream.concat(getTriples(ReferencesRdfContext.class));
            }

            rdfStream.concat(getTriples(ParentRdfContext.class));

            if (ldpPreferences.prefersContainment() || ldpPreferences.prefersMembership()) {
                rdfStream.concat(getTriples(ChildrenRdfContext.class));
            }

            if (ldpPreferences.prefersContainment()) {

                final Iterator<FedoraResource> children = resource().getChildren();

                rdfStream.concat(concat(transform(children,
                        new Function<FedoraResource, RdfStream>() {

                            @Override
                            public RdfStream apply(final FedoraResource child) {
                                return child.getTriples(translator(), PropertiesRdfContext.class);
                            }
                        })));

            }

            rdfStream.concat(getTriples(ContainerRdfContext.class));
        }
        
        returnPreference.addResponseHeaders(servletResponse);

        addResourceHttpHeaders(servletResponse, resource(), translator());

        addResponseInformationToStream(resource(), rdfStream, uriInfo,
                translator());

        return rdfStream;


    }

    /**
     * Creates a new object.
     *
     * application/octet-stream;qs=1001 is a workaround for JERSEY-2636, to ensure
     * requests without a Content-Type get routed here.
     *
     * @return 201
     */
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM + ";qs=1001", MediaType.WILDCARD})
    @Timed
    public Response createObject(@QueryParam("mixin") final String mixin,
                                 @QueryParam("checksum") final String checksum,
                                 @HeaderParam("Content-Disposition") final String contentDisposition,
                                 @HeaderParam("Content-Type") final MediaType requestContentType,
                                 @HeaderParam("Slug") final String slug,
                                 @ContentLocation final InputStream requestBodyStream)
            throws ParseException, IOException,
            InvalidChecksumException, URISyntaxException {
        init(uriInfo);

        if (resource().hasType(FEDORA_DATASTREAM)) {
            throw new ClientErrorException("Object cannot have child nodes", CONFLICT);
        }

        final MediaType contentType = getSimpleContentType(requestContentType);

        final String contentTypeString = contentType.toString();

        final String newObjectPath = mintNewPid(path, slug);

        LOGGER.debug("Attempting to ingest with path: {}", newObjectPath);

        try {

            final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
            final FedoraResource result = createFedoraResource(mixin,
                    effectiveContentType,
                    newObjectPath);

            final Response.ResponseBuilder response;
            final URI location = getUri(result);

            if (requestBodyStream == null || requestContentType == null) {
                LOGGER.trace("No request body detected");
                response = created(location).entity(location.toString());
            } else {
                LOGGER.trace("Received createObject with a request body and content type \"{}\"", contentTypeString);

                if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                    LOGGER.trace("Found SPARQL-Update content, applying..");
                    result.updatePropertiesDataset(translator(), IOUtils.toString(requestBodyStream));
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
                            createDefaultModel().read(requestBodyStream, getUri(result).toString(), format);

                    result.replaceProperties(translator(), inputModel,
                            getTriples(result, PropertiesRdfContext.class));
                    response = created(location).entity(location.toString());
                } else if (result instanceof Datastream) {
                    LOGGER.trace("Created a datastream and have a binary payload.");

                    final URI checksumURI = checksumURI(checksum);
                    final String originalFileName = originalFileName(contentDisposition);

                    final FedoraBinary binary = ((Datastream)result).getBinary();
                    binary.setContent(requestBodyStream,
                            contentTypeString,
                            checksumURI,
                            originalFileName,
                            datastreamService.getStoragePolicyDecisionPoint());

                    final URI contentLocation = getUri(binary);

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

    /**
     * Create a resource at a specified path, or replace triples with provided RDF.
     * @param requestContentType
     * @param requestBodyStream
     * @return 204
     */
    @PUT
    @Consumes({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, NTRIPLES, JSON_LD})
    @Timed
    public Response createOrReplaceObjectRdf(@HeaderParam("Content-Type") final MediaType requestContentType,
                                             @ContentLocation final InputStream requestBodyStream) throws URISyntaxException {
        init(uriInfo);

        LOGGER.debug("Attempting to replace path: {}", path);
        try {

            final FedoraResource resource;
            final Response.ResponseBuilder response;


            final MediaType contentType = getSimpleContentType(requestContentType);

            final boolean preexisting;
            if (nodeService.exists(session, path)) {
                resource = resource();
                response = noContent();
                preexisting = true;
            } else {
                final MediaType effectiveContentType
                        = requestBodyStream == null || requestContentType == null ? null : contentType;
                resource = createFedoraResource(null, effectiveContentType, path);

                final URI location = getUri(resource);

                response = created(location).entity(location.toString());
                preexisting = false;
            }

            evaluateRequestPreconditions(request, servletResponse, resource, session);

            if (requestContentType != null && requestBodyStream != null)  {
                final String format = contentTypeToLang(contentType.toString()).getName().toUpperCase();

                final Model inputModel = createDefaultModel()
                        .read(requestBodyStream, getUri(resource).toString(), format);

                resource.replaceProperties(translator(), inputModel,
                        getTriples(resource, PropertiesRdfContext.class));

            } else if (preexisting) {
                throw new ClientErrorException("No RDF provided and the resource already exists!", CONFLICT);
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
     * Update an object using SPARQL-UPDATE
     *
     * @return 201
     * @throws RepositoryException
     * @throws IOException
     */
    @PATCH
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@ContentLocation final InputStream requestBodyStream) throws IOException {

        init(uriInfo);
        LOGGER.debug("Attempting to update path: {}", path);

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        try {
            final String requestBody = IOUtils.toString(requestBodyStream);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            final Dataset properties = resource().updatePropertiesDataset(translator(), requestBody);

            handleProblems(properties);

            try {
                session.save();
                versionService.nodeUpdated(resource().getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, resource(), session);

            return noContent().build();

        } catch ( final RuntimeException ex ) {
            final Throwable cause = ex.getCause();
            if ( cause != null && cause instanceof PathNotFoundException ) {
                // the sparql update referred to a repository resource that doesn't exist
                throw new BadRequestException(cause.getMessage());
            }
            throw ex;
        } finally {
            session.logout();
        }
    }

    /**
     * Copies an object from one path to another
     */
    @COPY
    @Timed
    public Response copyObject(@HeaderParam("Destination") final String destinationUri)
            throws URISyntaxException {
        init(uriInfo);

        try {

            if (!nodeService.exists(session, path)) {
                throw new ClientErrorException("The source path does not exist", CONFLICT);
            }

            final String destination = getPath(destinationUri);

            if (destination == null) {
                throw new ServerErrorException("Destination was not a valid resource path", BAD_GATEWAY);
            } else if (nodeService.exists(session, destination)) {
                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED);
            }

            nodeService.copyObject(session, path, destination);

            session.save();
            versionService.nodeUpdated(session, destination);

            return created(new URI(destinationUri)).build();
        } catch (final RepositoryRuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof ItemExistsException) {

                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED, e);

            } else if (cause instanceof PathNotFoundException) {

                throw new ClientErrorException("There is no node that will serve as the parent of the copied item",
                        CONFLICT, e);
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
    public Response moveObject(@HeaderParam("Destination") final String destinationUri)
            throws URISyntaxException {
        init(uriInfo);

        try {

            if (!nodeService.exists(session, path)) {
                throw new ClientErrorException("The source path does not exist", CONFLICT);
            }


            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            final String destination = getPath(destinationUri);

            if (destination == null) {
                throw new ServerErrorException("Destination was not a valid resource path", BAD_GATEWAY);
            } else if (nodeService.exists(session, destination)) {
                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED);
            }

            nodeService.moveObject(session, path, destination);
            session.save();
            versionService.nodeUpdated(session, destination);
            return created(new URI(destinationUri)).build();
        } catch (final RepositoryRuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof ItemExistsException) {
                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED, e);
            } else if (cause instanceof PathNotFoundException) {
                throw new ClientErrorException("There is no node that will serve as the parent of the moved item",
                        CONFLICT, e);
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
     * Create a new object from a multipart/form-data POST request
     * @param mixin
     * @param slug
     * @param file
     * @return response
     */
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Timed
    public Response createObjectFromFormPost(@FormDataParam("mixin") final String mixin,
                                             @FormDataParam("slug") final String slug,
                                             @FormDataParam("file") final InputStream file
    ) throws URISyntaxException, InvalidChecksumException, ParseException, IOException {
        init(uriInfo);

        final MediaType effectiveContentType = file == null ? null : MediaType.APPLICATION_OCTET_STREAM_TYPE;
        return createObject(mixin, null, null, effectiveContentType, slug, file);

    }

    private RdfStream getTriples(final Class<? extends RdfStream> x) {
        return getTriples(resource(), x);
    }

    private RdfStream getTriples(final FedoraResource resource, final Class<? extends RdfStream> x) {
        return resource.getTriples(translator(), x);
    }

    private String mintNewPid(final String base, final String slug) {
        String pid;
        final String newObjectPath;

        assertPathExists(base);

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else {
            pid = pidMinter.mintPid();
        }
        // reverse translate the proffered or created identifier
        LOGGER.trace("Using external identifier {} to create new resource.", pid);
        LOGGER.trace("Using prefixed external identifier {} to create new resource.", uriInfo.getBaseUri() + "/"
                + pid);
        pid = translator().getPathFromSubject(createResource(uriInfo.getBaseUri() + "/" + pid));
        // remove leading slash left over from translation
        pid = pid.substring(1, pid.length());
        LOGGER.trace("Using internal identifier {} to create new resource.", pid);
        newObjectPath = base + "/" + pid;

        assertPathMissing(newObjectPath);
        return newObjectPath;
    }

    private FedoraResource createFedoraResource(final String requestMixin,
                                                final MediaType requestContentType,
                                                final String path) {
        final String objectType = getRequestedObjectType(requestMixin, requestContentType);

        final FedoraResource result;

        switch (objectType) {
            case FEDORA_OBJECT:
                result = objectService.findOrCreateObject(session, path);
                break;
            case FEDORA_DATASTREAM:
                result = datastreamService.findOrCreateDatastream(session, path);
                break;
            default:
                throw new ClientErrorException("Unknown object type " + objectType, BAD_REQUEST);
        }
        return result;
    }

    private void assertPathMissing(final String path) {
        if (nodeService.exists(session, path)) {
            throw new ClientErrorException(path + " is an existing resource!", CONFLICT);
        }
    }

    private void assertPathExists(final String path) {
        if (!nodeService.exists(session, path)) {
            throw new NotFoundException();
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

    private void handleProblems(final Dataset properties) {

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

            throw new ForbiddenException(error.length() > 0 ? error.toString() : problems.toString());
        }
    }

    /**
     * Method to check for any jcr namespace element in the path
     */
    @VisibleForTesting
    protected void throwIfPathIncludesJcr(final List<PathSegment> pathList) {
        if (pathList == null || pathList.size() == 0) {
            return;
        }
        final PathSegment pathSegment = pathList.get(pathList.size() - 1);
        final String[] tokens = pathSegment.getPath().split(":");
        if (tokens.length == 2 && tokens[0].equalsIgnoreCase("jcr")) {
            final String requestPath = uriInfo.getPath();
            LOGGER.trace("Request with jcr namespace is not allowed: {} ", requestPath);
            throw new NotFoundException();
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
