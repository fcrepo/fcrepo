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
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.springframework.stereotype.Component;


/**
 * Implementation of a factory for operations on rdf sources
 *
 * @author bbpennel
 */
@Component
public class RdfSourceOperationFactoryImpl implements RdfSourceOperationFactory {

    @Override
    public CreateRdfSourceOperationBuilder createBuilder(final FedoraId rescId, final String interactionModel) {
        return new CreateRdfSourceOperationBuilderImpl(rescId, interactionModel);
    }

    @Override
    public RdfSourceOperationBuilder updateBuilder(final FedoraId rescId) {
        return new UpdateRdfSourceOperationBuilder(rescId);
    }

}
