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
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * Transactions over REST
 *
 * @author awoods
 * @author gregjan
 * @author mohideen
 */
@Scope("prototype")
@Path("/fcr:tx")
public class Transactions extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(Transactions.class);

    @Inject
    private TransactionManager txManager;

    /**
     * Create a new transaction resource and add it to the registry
     *
     * @return 201 with the transaction id and expiration date
     * @throws URISyntaxException if URI syntax exception occurred
     */
    @POST
    public Response createTransaction() throws URISyntaxException {
        // TransactionProvider would have already created a transaction
        // Just, set short-lived to false
        transaction.setShortLived(false);
        LOGGER.info("Created transaction '{}'", transaction.getId());

        final Response.ResponseBuilder res = created(
                new URI(translator().toDomain("/tx:" + transaction.getId()).toString()));
        transaction.getExpires().ifPresent(expires -> {
            res.expires(from(expires));
        });
        return res.build();
    }

    /**
     * Commit a transaction resource
     *
     * @param txId the transaction id
     * @return 204
     */
    @PUT
    @Path("/fcr:tx/{transactionId}/commit")
    public Response commit(@PathParam("transactionId") final String txId) {
        try {
            final Transaction transaction = txManager.get(txId);
            LOGGER.info("Commit transaction '{}'", transaction.getId());
            transaction.commit();
            return noContent().build();
        } catch(Exception e) {
            if (e.getMessage().matches("No Transaction found with transactionId")) {
                return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Rollback a transaction
     *
     * @param txId the transaction id
     * @return 204
     */
    @DELETE
    @Path("/fcr:tx/{transactionId}")
    public Response rollback(@PathParam("transactionId") final String txId) {
        try {
            final Transaction transaction = txManager.get(txId);
            LOGGER.info("Rollback transaction '{}'", transaction.getId());
            transaction.rollback();
            return noContent().build();
        } catch(Exception e) {
            if (e.getMessage().matches("No Transaction found with transactionId")) {
                return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

}
