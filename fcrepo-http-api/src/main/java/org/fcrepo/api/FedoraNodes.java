
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.common.collection.Problems;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.update.UpdateAction;

@Component
@Path("/rest/{path: .*}")
public class FedoraNodes extends AbstractResource {

    private static final Logger logger = getLogger(FedoraNodes.class);

    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON,
            NTRIPLES, TEXT_HTML})
    public Dataset describe(@PathParam("path")
    final List<PathSegment> pathList, @Context
    final Request request, @Context UriInfo uriInfo) throws RepositoryException, IOException {
        final String path = toPath(pathList);
        logger.trace("Getting profile for {}", path);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);

            final Date date = resource.getLastModifiedDate();
            final Date roundedDate = new Date();
            if (date != null) {
                roundedDate.setTime(date.getTime() - date.getTime() % 1000);
            }
            final ResponseBuilder builder =
                    request.evaluatePreconditions(roundedDate);
            if (builder != null) {
                final CacheControl cc = new CacheControl();
                cc.setMaxAge(0);
                cc.setMustRevalidate(true);
                // here we are implicitly emitting a 304
                // the exception is not an error, it's genuinely
                // an exceptional condition
                throw new WebApplicationException(builder.cacheControl(cc)
                        .lastModified(date).build());
            }
            final HttpGraphSubjects subjects = new HttpGraphSubjects(FedoraNodes.class, uriInfo);
            final Dataset propertiesDataset = resource.getPropertiesDataset(subjects);
            addResponseInformationToDataset(resource, propertiesDataset, uriInfo, subjects);

            return propertiesDataset;

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
    final List<PathSegment> pathList, @Context
    final UriInfo uriInfo, final InputStream requestBodyStream)
            throws RepositoryException, IOException {
        final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        logger.debug("Modifying object with path: {}", path);

        try {

            final FedoraResource result = nodeService.getObject(session, path);

            if (requestBodyStream != null) {
                UpdateAction.parseExecute(IOUtils.toString(requestBodyStream),
                        result.getPropertiesDataset(new HttpGraphSubjects(
                                                                                 FedoraNodes.class, uriInfo)));
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
    final List<PathSegment> pathList, @Context
    final UriInfo uriInfo, final InputStream requestBodyStream)
            throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();

        try {

            if (requestBodyStream != null) {

                final FedoraResource result =
                        nodeService.getObject(session, path);

                result.updatePropertiesDataset(new HttpGraphSubjects(FedoraNodes.class,
                                                                            uriInfo), IOUtils.toString(requestBodyStream));
                final Problems problems = result.getDatasetProblems();
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
    final MediaType requestContentType, @Context
    final UriInfo uriInfo, final InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();

        try {
            if (nodeService.exists(session, path)) {
                return Response.status(HttpStatus.SC_CONFLICT).entity(
                        path + " is an existing resource").build();
            }

            createObjectOrDatastreamFromRequestContent(FedoraNodes.class,
                    session, path, mixin, uriInfo, requestBodyStream,
                    requestContentType, checksumType, checksum);

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

}
