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
package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.net.URI;
import java.util.Collection;

/**
 * Builder for operations to create non-rdf sources
 *
 * @author bbpennel
 */
public interface CreateNonRdfSourceOperationBuilder extends NonRdfSourceOperationBuilder {

    @Override
    CreateNonRdfSourceOperationBuilder mimeType(String mimetype);

    @Override
    CreateNonRdfSourceOperationBuilder filename(String filename);

    @Override
    CreateNonRdfSourceOperationBuilder contentDigests(Collection<URI> digests);

    @Override
    CreateNonRdfSourceOperationBuilder contentSize(long size);

    /**
     * Set the parent identifier of the resource
     *
     * @param parentId parent internal identifier
     * @return the builder
     */
    CreateNonRdfSourceOperationBuilder parentId(FedoraId parentId);

    @Override
    CreateNonRdfSourceOperationBuilder userPrincipal(String userPrincipal);

}
