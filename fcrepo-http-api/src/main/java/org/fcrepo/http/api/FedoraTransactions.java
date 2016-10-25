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

import static java.util.Date.from;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.services.BatchService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * Transactions over REST
 *
 * @author awoods
 * @author gregjan
 */
@Scope("prototype")
@Path("/{path: .*}/fcr:tx")
public class FedoraTransactions extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraTransactions.class);

    @Inject
    protected BatchService batchService;

    /**
     * Create a new transaction resource and add it to the registry
     *
     * @param externalPath the external path
     * @return 201 with the transaction id and expiration date
     * @throws URISyntaxException if URI syntax exception occurred
     */
    @POST
    public Response createTransaction(@PathParam("path") final String externalPath) throws URISyntaxException {

        if (batchService.exists(session.getId(), getUserPrincipal())) {
            LOGGER.debug("renewing transaction {}", session.getId());
            batchService.refresh(session.getId(), getUserPrincipal());
            final Response.ResponseBuilder res = noContent();
            session.getFedoraSession().getExpires().ifPresent(expires -> {
                res.expires(from(expires));
            });
            return res.build();
        }

        if (externalPath != null && !externalPath.isEmpty()) {
            return status(BAD_REQUEST).build();
        }

        batchService.begin(session.getFedoraSession(), getUserPrincipal());
        session.makeBatchSession();
        LOGGER.info("Created transaction '{}'", session.getId());

        final Response.ResponseBuilder res = created(
                new URI(translator().toDomain("/tx:" + session.getId()).toString()));
        session.getFedoraSession().getExpires().ifPresent(expires -> {
            res.expires(from(expires));
        });
        return res.build();
    }

    /**
     * Commit a transaction resource
     *
     * @param externalPath the external path
     * @return 204
     */
    @POST
    @Path("fcr:commit")
    public Response commit(@PathParam("path") final String externalPath) {
        LOGGER.info("Commit transaction '{}'", externalPath);
        return finalizeTransaction(externalPath, getUserPrincipal(), true);
    }

    /**
     * Rollback a transaction
     *
     * @param externalPath the external path
     * @return 204
     */
    @POST
    @Path("fcr:rollback")
    public Response rollback(@PathParam("path") final String externalPath) {

        LOGGER.info("Rollback transaction '{}'", externalPath);
        return finalizeTransaction(externalPath, getUserPrincipal(), false);
    }

    private Response finalizeTransaction(@PathParam("path")
        final String externalPath, final String username, final boolean commit) {

        final String path = toPath(translator(), externalPath);
        if (!path.equals("/")) {
            return status(BAD_REQUEST).build();
        }

        if (!session.isBatchSession()) {
            LOGGER.debug("cannot finalize an empty tx id {} at path {}", session.getId(), path);
            return status(BAD_REQUEST).build();
        }

        if (commit) {
            LOGGER.debug("commiting transaction {} at path {}", session.getId(), path);
            batchService.commit(session.getId(), username);

        } else {
            LOGGER.debug("rolling back transaction {} at path {}", session.getId(), path);
            batchService.abort(session.getId(), username);
        }
        return noContent().build();
    }
}
