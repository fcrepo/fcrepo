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

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.http.commons.session.TransactionProvider;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraBaseResource.class);

    @Context
    protected SecurityContext securityContext;

    @Inject
    protected ResourceFactory resourceFactory;

    @Inject
    protected ResourceHelper resourceHelper;

    @Context protected HttpServletRequest servletRequest;

    @Inject
    protected TransactionManager txManager;

    private TransactionProvider txProvider;

    private Transaction transaction;

    protected HttpIdentifierConverter identifierConverter;

    protected HttpIdentifierConverter identifierConverter() {
        if (identifierConverter == null) {
            identifierConverter = new HttpIdentifierConverter(
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return identifierConverter;
    }

    /**
     * Gets a fedora resource by id. Uses the provided transaction if it is uncommitted,
     * or uses a new transaction.
     *
     * @param transaction the fedora transaction
     * @param fedoraId    identifier of the resource
     * @return the requested FedoraResource
     */
    protected FedoraResource getFedoraResource(final Transaction transaction, final FedoraId fedoraId)
            throws PathNotFoundException {
        return resourceFactory.getResource(transaction, fedoraId);
    }

    /**
     * @param transaction the transaction in which to check
     * @param fedoraId identifier of the object to check
     * @param includeDeleted Whether to check for deleted resources too.
     * @return Returns true if an object with the provided id exists
     */
    protected boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                        final boolean includeDeleted) {
        return resourceHelper.doesResourceExist(transaction, fedoraId, includeDeleted);
    }

    /**
     *
     * @param transaction the transaction in which to check
     * @param fedoraId identifier of the object to check
     * @return Returns true if object does not exist but whose ID starts other resources that do exist.
     */
    protected boolean isGhostNode(final Transaction transaction, final FedoraId fedoraId) {
        return resourceHelper.isGhostNode(transaction, fedoraId);
    }

    protected String getUserPrincipal() {
        final Principal p = securityContext.getUserPrincipal();
        return p == null ? null : p.getName();
    }

    protected Transaction transaction() {
        if (transaction == null) {
            txProvider = new TransactionProvider(txManager, servletRequest, uriInfo.getBaseUri());
            transaction = txProvider.provide();
        }
        return transaction;
    }

}
