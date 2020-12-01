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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface UpdatePropertiesService {
  /**
   * Update the provided properties with a SPARQL Update query. The updated
   * properties may be serialized to the persistence layer.
   *
   * @param tx the Transaction
   * @param userPrincipal the user performing the service
   * @param fedoraId the internal Id of the fedora resource to update
   * @param sparqlUpdateStatement sparql update statement
   * @throws MalformedRdfException if malformed rdf exception occurred
   * @throws AccessDeniedException if access denied in updating properties
   */
  void updateProperties(final Transaction tx,
                        final String userPrincipal,
                        final FedoraId fedoraId,
                        final String sparqlUpdateStatement) throws MalformedRdfException, AccessDeniedException;
}
