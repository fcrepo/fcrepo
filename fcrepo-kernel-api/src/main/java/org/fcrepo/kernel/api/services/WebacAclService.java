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

package org.fcrepo.kernel.api.services;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.WebacAcl;

/**
 * Service for creating and retrieving {@link WebacAcl}
 *
 * @author peichman
 * @author whikloj
 * @since 6.0.0
 */
public interface WebacAclService {

    /**
     * Retrieve an existing WebACL by transaction and path
     *
     * @param fedoraId the fedoraID to the resource the ACL is part of
     * @param transaction the transaction
     * @return retrieved ACL
     */
    WebacAcl find(final Transaction transaction, final FedoraId fedoraId);

    /**
     * Retrieve or create a new WebACL by transaction and path
     *
     * @param transaction the transaction
     * @param fedoraId the fedoraID to the resource the ACL is part of
     * @param userPrincipal the user creating the ACL.
     * @param model the contents of the ACL RDF.
     */
    void create(final Transaction transaction, final FedoraId fedoraId, final String userPrincipal,
                    final Model model);
}
