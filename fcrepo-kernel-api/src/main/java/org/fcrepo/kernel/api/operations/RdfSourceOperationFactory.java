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


import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Factory for operations on rdf sources
 *
 * @author bbpennel
 */
public interface RdfSourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for an operation to create an RDF source
     *
     * @param rescId id of the resource targeted by the operation
     * @param interactionModel interaction model for the resource being created
     * @param serverManagedPropsMode server managed props mode
     * @return new builder
     */
    CreateRdfSourceOperationBuilder createBuilder(FedoraId rescId,
                                                  String interactionModel,
                                                  ServerManagedPropsMode serverManagedPropsMode);

    /**
     * Get a builder for an operation to update an RDF source
     *
     * @param rescId id of the resource targeted by the operation
     * @param serverManagedPropsMode server managed props mode
     * @return new builder
     */
    RdfSourceOperationBuilder updateBuilder(FedoraId rescId, final ServerManagedPropsMode serverManagedPropsMode);

}
