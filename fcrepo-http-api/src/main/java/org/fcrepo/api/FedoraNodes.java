
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.riot.WebContent;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraObject;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.provider.GraphStreamingOutput;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.common.collection.Problems;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.update.UpdateAction;

@Component
@Path("/rest/{path: .*}")
public class FedoraNodes extends AbstractResource {

    private static final Logger logger = getLogger(FedoraNodes.class);

    @Autowired
    private LowLevelStorageService llStoreService;

    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Response describeRdf(@PathParam("path")
    final List<PathSegment> pathList, @Context
    final Request request) throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.trace("Getting profile for {}", path);

        final Variant bestPossibleResponse =
                request.selectVariant(POSSIBLE_RDF_VARIANTS);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);

            final Date date = resource.getLastModifiedDate();
            final Date roundedDate = new Date();
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);

            Response.ResponseBuilder builder =
                    request.evaluatePreconditions(roundedDate);

            if (builder == null) {
                builder =
                        ok(new GraphStreamingOutput(resource.getGraphStore(),
                                bestPossibleResponse.getMediaType()));
            }

            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);

            return builder.cacheControl(cc).lastModified(date).build();

        } finally {
            session.logout();
        }

    }

    /**
     * Does nothing (good) yet -- just runs SPARQL-UPDATE statements
     * @param pathList
     * @param requestBodyStream
     * @return 201
     * @throws RepositoryException
     */
    @PUT
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response modifyObject(@PathParam("path")
    final List<PathSegment> pathList, final InputStream requestBodyStream)
            throws RepositoryException, IOException {
        final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        logger.debug("Modifying object with path: {}", path);

        try {

            final FedoraResource result = nodeService.getObject(session, path);

            if (requestBodyStream != null) {
                UpdateAction.parseExecute(IOUtils.toString(requestBodyStream),
                        result.getGraphStore());
            }
            session.save();

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Update an object using SPARQL-UPDATE
     *
     * @param pathList
     * @return 201
     * @throws RepositoryException
     * @throws org.fcrepo.exception.InvalidChecksumException
     * @throws IOException
     */
    @POST
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@PathParam("path")
    final List<PathSegment> pathList, final InputStream requestBodyStream)
            throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();

        try {

            if (requestBodyStream != null) {

                final FedoraResource result =
                        nodeService.getObject(session, path);

                result.updateGraph(IOUtils.toString(requestBodyStream));
                final Problems problems = result.getGraphProblems();
                if (problems != null && problems.hasProblems()) {
                    logger.info(
                            "Found these problems updating the properties for {}: {}",
                            path, problems.toString());
                    return status(Response.Status.FORBIDDEN).entity(
                            problems.toString()).build();

                }

                session.save();

                return Response.status(HttpStatus.SC_NO_CONTENT).build();
            } else {
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                        "SPARQL-UPDATE requests must have content ").build();
            }

        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new object.
     * 
     * @param pathList
     * @return 201
     * @throws RepositoryException
     * @throws InvalidChecksumException 
     * @throws IOException 
     */
    @POST
    @Timed
    public Response createObject(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("mixin")
    @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT)
    final String mixin, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();

        try {
            if (nodeService.exists(session, path)) {
                return Response.status(HttpStatus.SC_CONFLICT).entity(
                        path + " is an existing resource").build();
            }

            if (FedoraJcrTypes.FEDORA_OBJECT.equals(mixin)) {
                final FedoraObject result =
                        objectService.createObject(session, path);

                if (requestBodyStream != null &&
                        requestContentType != null &&
                        requestContentType.toString().equals(
                                WebContent.contentTypeSPARQLUpdate)) {
                    result.updateGraph(IOUtils.toString(requestBodyStream));
                }

            }
            if (FedoraJcrTypes.FEDORA_DATASTREAM.equals(mixin)) {
                final MediaType contentType =
                        requestContentType != null ? requestContentType
                                : APPLICATION_OCTET_STREAM_TYPE;

                datastreamService.createDatastreamNode(session, path,
                        contentType.toString(), requestBodyStream,
                        checksumType, checksum);
            }
            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(uriInfo.getRequestUri()).entity(path).build();

        } finally {
            session.logout();
        }
    }

    /**
     * Deletes an object.
     * 
     * @param path
     * @return
     * @throws RepositoryException
     */
    @DELETE
    @Timed
    public Response deleteObject(@PathParam("path")
    final List<PathSegment> path) throws RepositoryException {
        final Session session = getAuthenticatedSession();

        try {
            nodeService.deleteObject(session, toPath(path));
            session.save();
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    public ObjectService getObjectService() {
        return objectService;
    }

    public void setObjectService(final ObjectService objectService) {
        this.objectService = objectService;
    }

    public DatastreamService getDatastreamService() {
        return datastreamService;
    }

    public void setDatastreamService(final DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

    public LowLevelStorageService getLlStoreService() {
        return llStoreService;
    }

    public void setLlStoreService(final LowLevelStorageService llStoreService) {
        this.llStoreService = llStoreService;
    }

}
