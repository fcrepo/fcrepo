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

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_WEBAC_ACL;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * @author lsitu
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

    @Override
    protected String externalPath() {
        return externalPath;
    }
}
