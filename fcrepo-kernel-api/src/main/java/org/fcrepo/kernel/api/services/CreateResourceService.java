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
import java.util.List;

/**
 * Interface for a service to create a new resource via a POST request.
 * @author whikloj
 * @since 2019-11-05
 */
public interface CreateResourceService {

    /**
     * Create a new NonRdfSource resource.
     *
     * @param txId The transaction ID for the request.
     * @param fedoraId The internal identifier of the parent.
     * @param slug The Slug header or null if none.
     * @param contentType The content-type header or null if none.
     * @param linkHeaders The original LINK headers or null if none.
     * @param digest The binary digest or null if none.
     * @param requestBody The request body or null if none.
     * @param externalContent The external content handler or null if none.
     */
    void perform(final String txId, final String fedoraId, final String slug, final String contentType,
                 final List<String> linkHeaders, final Collection<String> digest,
                 final InputStream requestBody, final ExternalContent externalContent);

    /**
     * Create a new RdfSource resource.
     *
     * @param txId The transaction ID for the request.
     * @param fedoraId The internal identifier of the parent.
     * @param slug The Slug header or null if none.
     * @param linkHeaders The original LINK headers or null if none.
     * @param model The request body RDF as a Model
     */
    void perform(final String txId, final String fedoraId, final String slug, final String contentType,
                 final List<String> linkHeaders, final Model model);
}
