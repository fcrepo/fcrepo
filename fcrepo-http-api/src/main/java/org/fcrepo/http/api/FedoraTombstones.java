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

import com.google.common.annotations.VisibleForTesting;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CRUD operations on Fedora tombstones
 *
 * @author cbeer
 */
@Scope("request")
@Path("/{path: .*}/fcr:tombstone")
public class FedoraTombstones extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraTombstones.class);

    @Inject
    protected Session session;

    @PathParam("path") protected String externalPath;

    /**
     * Default JAX-RS entry point
     */
    public FedoraTombstones() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraTombstones(final String externalPath) {
        this.externalPath = externalPath;
    }


    /**
     * Delete a tombstone resource (freeing the original resource to be reused)
     * @return the free resource
     */
    @DELETE
    public Response delete() {
        LOGGER.info("Delete tombstone: {}", resource());
        resource().delete();

        try {
            session.save();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return noContent().build();
    }

    protected FedoraResource resource() {
        return translator().convert(translator().toDomain(externalPath));
    }


    @Override
    protected Session session() {
        return session;
    }

}
