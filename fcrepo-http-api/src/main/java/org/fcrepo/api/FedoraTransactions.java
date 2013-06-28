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

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.fcrepo.TxSession;
import org.fcrepo.services.TransactionService;
import org.fcrepo.session.InjectedSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraTransactions.class);


    @Autowired
    private TransactionService txService;

    @InjectedSession
    protected Session session;

    @POST
    public Response createTransaction(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {

        LOGGER.debug("creating transaction at path {}", pathList);

        if (!pathList.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (session instanceof TxSession) {
            Transaction t =
                    txService.getTransaction(((TxSession) session).getTxId());
            t.updateExpiryDate();
            return noContent().expires(t.getExpires()).build();

        } else {
            Transaction t = txService.beginTransaction(session);
            return created(
                    uriInfo.getBaseUriBuilder().path(FedoraNodes.class)
                            .buildFromMap(
                                    ImmutableMap.of("path", "tx:" + t.getId())))
                    .expires(t.getExpires()).build();
        }
    }

    @POST
    @Path("fcr:commit")
    public Response commit(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);

        final String txId;
        if (session instanceof TxSession) {
            txId = ((TxSession) session).getTxId();
        } else {
            txId = "";
        }

        LOGGER.debug("committing transaction {} at path {}", txId, path);

        if (!path.equals("/")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!txId.isEmpty()) {
            txService.commit(txId);
            return noContent().build();

        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

    @POST
    @Path("fcr:rollback")
    public Response rollback(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);

        final String txId;
        if (session instanceof TxSession) {
            txId = ((TxSession) session).getTxId();
        } else {
            txId = "";
        }

        LOGGER.debug("committing transaction {} at path {}", txId, path);


        if (!path.equals("/")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!txId.isEmpty()) {
            txService.rollback(txId);
            return noContent().build();

        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
