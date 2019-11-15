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

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface UpdatePropertiesService {
  /**
   * Update the provided properties with a SPARQL Update query. The updated
   * properties may be serialized to the persistence layer.
   *
   * @param fedoraResource the Fedora resource to update
   * @param sparqlUpdateStatement sparql update statement
   * @param originalTriples original triples
   * @throws MalformedRdfException if malformed rdf exception occurred
   * @throws AccessDeniedException if access denied in updating properties
   */
  void updateProperties(final FedoraResource fedoraResource,
                        final String sparqlUpdateStatement,
                        final RdfStream originalTriples) throws MalformedRdfException, AccessDeniedException;
}
