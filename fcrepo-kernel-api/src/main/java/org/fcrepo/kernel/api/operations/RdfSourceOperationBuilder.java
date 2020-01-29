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

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;

/**
 * Builder for constructing an RdfSourceOperation
 *
 * @author bbpennel
 */
public interface RdfSourceOperationBuilder extends ResourceOperationBuilder {

    @Override
    RdfSourceOperationBuilder userPrincipal(String userPrincipal);

    @Override
    RdfSourceOperation build();

    /**
     * Set the triples for the operation
     *
     * @param triples the resource's triples
     * @return this builder
     */
    RdfSourceOperationBuilder triples(RdfStream triples);

    /**
     * Set the relaxed managed properties for this resource if the server
     * is in relaxed mode.
     *
     * @param model rdf of the resource
     * @return this builder
     */
    RdfSourceOperationBuilder relaxedProperties(Model model);

}
