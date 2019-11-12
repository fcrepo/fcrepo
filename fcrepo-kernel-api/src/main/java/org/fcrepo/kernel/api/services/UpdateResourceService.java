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
import org.fcrepo.kernel.api.models.ExternalContent;

import java.io.InputStream;
import java.util.Collection;

/**
 * Interface for a service to update an existing resource via a PUT request.
 * @author mohideen
 * @since 2019-11-07
 */
public interface UpdateResourceService {

    /**
     * Update an existing resource.
     *
     * @param txId The transaction ID for the request.
     * @param fedoraId The internal identifier of the parent.
     * @param filename The filename of the binary.
     * @param contentType The content-type header or null if none.
     * @param digest The binary digest or null if none.
     * @param size The binary size.
     * @param requestBody The request body or null if none.
     * @param externalContent The external content handler or null if none.
     */
    void perform(final String txId, final String fedoraId, final String filename,
                 final String contentType, final Collection<String> digest, final InputStream requestBody,
                 final long size, final ExternalContent externalContent);


    /**
     * Update a RdfSource resource.
     *
     * @param txId The transaction ID for the request.
     * @param fedoraId The internal identifier of the parent.
     * @param model The request body RDF as a Model
     */
    void perform(final String txId, final String fedoraId, final String contentType, final Model model);
}