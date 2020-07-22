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

import java.io.InputStream;
import java.net.URI;

/**
 * Factory for constructing operations on non-rdf sources
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for a external binary update operation
     *
     * @param rescId id of the resource targeted by the operation
     * @param handling the type of handling to be used for the external binary content
     * @param contentUri the URI of the external binary content
     * @return a new builder
     */
    NonRdfSourceOperationBuilder updateExternalBinaryBuilder(FedoraId rescId, String handling, URI contentUri);

    /**
     * Get a builder for an internal binary update operation
     *
     * @param rescId id of the resource targeted by the operation
     * @param contentStream inputstream for the content of this binary
     * @return a new builder
     */
    NonRdfSourceOperationBuilder updateInternalBinaryBuilder(FedoraId rescId, InputStream contentStream);

    /**
     * Get a builder for a external binary create operation
     *
     * @param rescId id of the resource targeted by the operation
     * @param handling the type of handling to be used for the external binary content
     * @param contentUri the URI of the external binary content
     * @return a new builder
     */
    CreateNonRdfSourceOperationBuilder createExternalBinaryBuilder(FedoraId rescId, String handling, URI contentUri);

    /**
     * Get a builder for an internal binary create operation
     *
     * @param rescId id of the resource targeted by the operation
     * @param contentStream inputstream for the content of this binary
     * @return a new builder
     */
    CreateNonRdfSourceOperationBuilder createInternalBinaryBuilder(FedoraId rescId, InputStream contentStream);
}
