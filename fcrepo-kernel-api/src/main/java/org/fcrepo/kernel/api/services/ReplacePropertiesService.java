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

import java.io.InputStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface ReplacePropertiesService {
  /**
   * Replace the properties of this object with the properties from the given
   * model
   *
   * @param tx the Transaction
   * @param fedoraResource the Fedora resource to update
   * @param requestBodyStream the body of the request
   * @param contentType the original triples
   * @throws MalformedRdfException if malformed rdf exception occurred
   */
  void replaceProperties(final Transaction tx,
                         final FedoraResource fedoraResource,
                         final InputStream requestBodyStream,
                         final String contentType) throws MalformedRdfException;
}
