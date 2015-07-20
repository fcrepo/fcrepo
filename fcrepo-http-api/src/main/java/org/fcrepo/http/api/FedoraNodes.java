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

import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.domain.COPY;
import org.fcrepo.http.commons.domain.MOVE;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * CRUD operations on Fedora Nodes
 *
 * @author cbeer
 */
@Scope("request")
@Path("/{path: .*}")
public class FedoraNodes extends ContentExposingResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraNodes.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected String externalPath;

    protected FedoraResource resource;

    /**
     * Default JAX-RS entry point
     */
    public FedoraNodes() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraNodes(final String externalPath) {
        this.externalPath = externalPath;
    }


    /**
     * Run these actions after initializing this resource
     */
    @PostConstruct
    public void postConstruct() {
        setUpJMSInfo(uriInfo, headers);
    }

    /**
     * Copies an object from one path to another
     * @param destinationUri the destination uri
     * @throws URISyntaxException if uri syntax exception occurred
     * @return the response
     */
    @COPY
    @Timed
    public Response copyObject(@HeaderParam("Destination") final String destinationUri)
            throws URISyntaxException {

        try {
            final String source = translator().asString(translator().toDomain(externalPath));

            if (!nodeService.exists(session, source)) {
                throw new ClientErrorException("The source path does not exist", CONFLICT);
            }

            final String destination = translator().asString(ResourceFactory.createResource(destinationUri));

            if (destination == null) {
                throw new ServerErrorException("Destination was not a valid resource path", BAD_GATEWAY);
            } else if (nodeService.exists(session, destination)) {
                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED);
            }

            LOGGER.info("Copy from '{}' to '{}'", source, destination);
            nodeService.copyObject(session, source, destination);

            session.save();

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
        }

    }

    /**
     * Copies an object from one path to another
     * @param destinationUri the destination uri
     * @throws URISyntaxException if uri syntax exception occurred
     * @return the response
     */
    @MOVE
    @Timed
    public Response moveObject(@HeaderParam("Destination") final String destinationUri)
            throws URISyntaxException {

        try {

            final String source = toPath(translator(), externalPath);

            if (!nodeService.exists(session, source)) {
                throw new ClientErrorException("The source path does not exist", CONFLICT);
            }


            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            final String destination = translator().asString(ResourceFactory.createResource(destinationUri));

            if (destination == null) {
                throw new ServerErrorException("Destination was not a valid resource path", BAD_GATEWAY);
            } else if (nodeService.exists(session, destination)) {
                throw new ClientErrorException("Destination resource already exists", PRECONDITION_FAILED);
            }

            LOGGER.info("Move from '{}' to '{}'", source, destination);
            nodeService.moveObject(session, resource().getPath(), destination);
            session.save();
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
        }
    }

    @Override
    protected Session session() {
        return session;
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

}
