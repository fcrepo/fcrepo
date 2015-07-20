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

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TxSession;
import org.fcrepo.kernel.api.services.TransactionService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TransactionService txService;

    @Inject
    protected Session session;

    /**
     * Create a new transaction resource and add it to the registry
     *
     * @param externalPath the external path
     * @param req the http servlet request
     * @return 201 with the transaction id and expiration date
     * @throws URISyntaxException if URI syntax exception occurred
     */
    @POST
    public Response createTransaction(@PathParam("path") final String externalPath,
                                      @Context final HttpServletRequest req) throws URISyntaxException {

        if (session instanceof TxSession) {
            final Transaction t = txService.getTransaction(session);
            LOGGER.debug("renewing transaction {}", t.getId());
            t.updateExpiryDate();
            return noContent().expires(t.getExpires()).build();
        }

        if (externalPath != null && !externalPath.isEmpty()) {
            return status(BAD_REQUEST).build();
        }

        final Principal userPrincipal = req.getUserPrincipal();
        String userName = null;
        if (userPrincipal != null) {
            userName = userPrincipal.getName();
        }

        final Transaction t = txService.beginTransaction(session, userName);
        LOGGER.info("Created transaction '{}'", t.getId());

        return created(new URI(translator().toDomain("/tx:" + t.getId()).toString())).expires(
                t.getExpires()).build();
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
        return finalizeTransaction(externalPath, true);

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
        return finalizeTransaction(externalPath, false);
    }

    private Response finalizeTransaction(@PathParam("path")
        final String externalPath, final boolean commit) {

        final String path = toPath(translator(), externalPath);
        if (!path.equals("/")) {
            return status(BAD_REQUEST).build();
        }

        final String txId;
        if (session instanceof TxSession) {
            txId = ((TxSession) session).getTxId();
        } else {
            txId = "";
        }

        if (txId.isEmpty()) {
            LOGGER.debug("cannot finalize an empty tx id {} at path {}",
                    txId, path);
            return status(BAD_REQUEST).build();
        }

        if (commit) {
            LOGGER.debug("commiting transaction {} at path {}", txId, path);
            txService.commit(txId);

        } else {
            LOGGER.debug("rolling back transaction {} at path {}", txId,
                    path);
            txService.rollback(txId);
        }
        return noContent().build();
    }

    @Override
    protected Session session() {
        return session;
    }
}
