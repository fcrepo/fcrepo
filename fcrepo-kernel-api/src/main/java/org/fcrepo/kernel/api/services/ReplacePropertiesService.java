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
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.FedoraID;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface ReplacePropertiesService {

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param txId the Transaction Id
     * @param userPrincipal the user performing the service
     * @param fedoraId the internal Id of the fedora resource to update
     * @param contentType the original triples
     * @param inputModel the model built from the body of the request
     * @throws MalformedRdfException if malformed rdf exception occurred
     */
    void perform(String txId,
                String userPrincipal,
                FedoraID fedoraId,
                String contentType,
                Model inputModel) throws MalformedRdfException;
}
