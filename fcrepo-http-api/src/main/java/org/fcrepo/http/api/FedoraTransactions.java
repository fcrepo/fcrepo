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

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.Principal;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.TxSession;
import org.fcrepo.kernel.services.TransactionService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Transactions over REST
 *
 * @author awoods
 * @author gregjan
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraTransactions.class);

    @Autowired
    private TransactionService txService;

    @InjectedSession
    protected Session session;

    /**
     * Create a new transaction resource and add it to the registry
     *
     * @param pathList
     * @return 201 with the transaction id and expiration date
     * @throws RepositoryException
     */
    @POST
    public Response createTransaction(@PathParam("path") final List<PathSegment> pathList,
                                      @Context final HttpServletRequest req) throws RepositoryException {

        if (session instanceof TxSession) {
            final Transaction t = txService.getTransaction(session);
            LOGGER.debug("renewing transaction {}", t.getId());
            t.updateExpiryDate();
            return noContent().expires(t.getExpires()).build();
        }

        if (!pathList.isEmpty()) {
            return status(BAD_REQUEST).build();
        }

        final Principal userPrincipal = req.getUserPrincipal();
        String userName = null;
        if (userPrincipal != null) {
            userName = userPrincipal.getName();
        }

        final Transaction t = txService.beginTransaction(session, userName);
        LOGGER.debug("created transaction {}", t.getId());

        return created(
                uriInfo.getBaseUriBuilder().path(FedoraNodes.class)
                        .buildFromMap(
                                singletonMap("path", "tx:" +
                                        t.getId()))).expires(
                t.getExpires()).build();
    }

    /**
     * Commit a transaction resource
     *
     * @param pathList
     * @return 204
     * @throws RepositoryException
     */
    @POST
    @Path("fcr:commit")
    public Response commit(@PathParam("path")
        final List<PathSegment> pathList) throws RepositoryException {

        return finalizeTransaction(pathList, true);

    }

    /**
     * Rollback a transaction
     * @return 204
     */
    @POST
    @Path("fcr:rollback")
    public Response rollback(@PathParam("path")
        final List<PathSegment> pathList) throws RepositoryException {

        return finalizeTransaction(pathList, false);
    }

    private Response finalizeTransaction(@PathParam("path")
        final List<PathSegment> pathList, final boolean commit)
        throws RepositoryException {

        final String path = toPath(pathList);
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
}
