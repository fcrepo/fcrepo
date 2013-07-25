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

package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.RdfLexicon.FIRST_PAGE;
import static org.fcrepo.RdfLexicon.HAS_CHILD_COUNT;
import static org.fcrepo.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.fcrepo.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.io.IOUtils;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.session.InjectedSession;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * CRUD operations on Fedora Nodes
 */
@Component
@Scope("prototype")
@Path("/{path: .*}")
public class FedoraNodes extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger logger = getLogger(FedoraNodes.class);

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
     * @throws IOException
     */
    @GET
    @Produces({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, RDF_JSON, NTRIPLES,
            TEXT_HTML})
    public Dataset describe(@PathParam("path")
            final List<PathSegment> pathList,
            @QueryParam("offset")
            @DefaultValue("0")
            final long offset,
            @QueryParam("limit")
            @DefaultValue("-1")
            final int limit,
            @QueryParam("non-member-properties")
            final String nonMemberProperties,
            @Context
            final Request request,
            @Context
            final UriInfo uriInfo) throws RepositoryException, IOException {
        final String path = toPath(pathList);
        logger.trace("Getting profile for {}", path);

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
            final HttpGraphSubjects subjects =
                    new HttpGraphSubjects(FedoraNodes.class, uriInfo);

            final int realLimit;
            if (nonMemberProperties != null && limit == -1) {
                realLimit = -2;
            } else {
                realLimit = limit;
            }

            final Dataset propertiesDataset =
                    resource.getPropertiesDataset(subjects, offset, realLimit);

            if (limit > 0 && propertiesDataset.getDefaultModel()
                    .contains(subjects.getGraphSubject(resource.getNode()),
                                 HAS_CHILD_COUNT)) {

                Model requestModel = ModelFactory.createDefaultModel();

                final long childCount = propertiesDataset.getDefaultModel()
                                            .listObjectsOfProperty(subjects.getGraphSubject(resource.getNode()), HAS_CHILD_COUNT)
                                            .nextNode().asLiteral().getLong();

                if (childCount > (offset + limit)) {

                    final Resource nextPageResource =
                        requestModel.createResource(uriInfo
                                                       .getRequestUriBuilder()
                                                       .queryParam("offset", offset + limit)
                                                       .queryParam("limit", limit)
                                                       .toString());
                    requestModel.add(subjects.getContext(), NEXT_PAGE, nextPageResource);
                }

                final Resource firstPageResource =
                    requestModel.createResource(uriInfo
                                                    .getRequestUriBuilder()
                                                    .queryParam("offset", 0)
                                                    .queryParam("limit", limit)
                                                    .toString());
                requestModel.add(subjects.getContext(), FIRST_PAGE, firstPageResource);

                propertiesDataset.addNamedModel("requestModel", requestModel);

            }

            addResponseInformationToDataset(resource, propertiesDataset,
                    uriInfo, subjects);

            return propertiesDataset;

        } finally {
            session.logout();
        }

    }

    /**
     * Does nothing (good) yet -- just runs SPARQL-UPDATE statements
     * 
     * @param pathList
     * @param requestBodyStream
     * @return 201
     * @throws RepositoryException
     */
    @PUT
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response modifyObject(
            @PathParam("path")
            final List<PathSegment> pathList,
            @Context
            final UriInfo uriInfo,
            final InputStream requestBodyStream,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            @Context
            final Request request) throws RepositoryException, IOException,
        InvalidChecksumException {
        final String path = toPath(pathList);
        logger.debug("Modifying object with path: {}", path);

        try {
            final FedoraResource resource =
                    nodeService.findOrCreateObject(session, path);

            final boolean isNew = resource.isNew();

            if (!isNew) {
                final Date date = resource.getLastModifiedDate();
                final Date roundedDate = new Date();
                roundedDate.setTime(date.getTime() - date.getTime() % 1000);
                final ResponseBuilder builder =
                        request.evaluatePreconditions(roundedDate);

                if (builder != null) {
                    throw new WebApplicationException(builder.build());
                }
            }

            resource.updatePropertiesDataset(new HttpGraphSubjects(
                    FedoraNodes.class, uriInfo), IOUtils
                    .toString(requestBodyStream));
            session.save();

            if (isNew) {
                return created(
                        uriInfo.getBaseUriBuilder().path(FedoraNodes.class)
                                .build(path.substring(1))).build();
            } else {
                return noContent().build();
            }

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
            final List<PathSegment> pathList,
            @Context
            final UriInfo uriInfo,
            final InputStream requestBodyStream)
        throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        try {

            if (requestBodyStream != null) {

                final FedoraResource result =
                        nodeService.getObject(session, path);

                Dataset properties = result.updatePropertiesDataset(new HttpGraphSubjects(
                        FedoraNodes.class, uriInfo), IOUtils
                        .toString(requestBodyStream));
                Model problems = properties.getNamedModel(PROBLEMS_MODEL_NAME);
                if (problems.size() > 0) {
                    logger.info(
                            "Found these problems updating the properties for {}: {}",
                            path, problems);
                    return status(FORBIDDEN).entity(problems.toString())
                            .build();

                }

                session.save();

                return status(SC_NO_CONTENT).build();
            } else {
                return status(SC_BAD_REQUEST).entity(
                        "SPARQL-UPDATE requests must have content!").build();
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
            final List<PathSegment> pathList,
            @QueryParam("mixin")
            @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT)
            final String mixin,
            @QueryParam("checksum")
            final String checksum,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            @Context
            final UriInfo uriInfo, final InputStream requestBodyStream)
        throws RepositoryException, IOException, InvalidChecksumException, URISyntaxException {

        final String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        try {
            if (nodeService.exists(session, path)) {
                return status(SC_CONFLICT).entity(
                        path + " is an existing resource!").build();
            }


            final URI checksumURI;

            if (checksum != null && !checksum.equals("")) {
                checksumURI = new URI(checksum);
            } else {
                checksumURI = null;
            }

            createObjectOrDatastreamFromRequestContent(FedoraNodes.class,
                    session, path, mixin, uriInfo, requestBodyStream,
                    requestContentType, checksumURI);

            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(uriInfo.getRequestUri()).entity(path.substring(1))
                    .build();

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

        try {
            nodeService.deleteObject(session, toPath(path));
            session.save();
            return noContent().build();
        } finally {
            session.logout();
        }
    }
}
