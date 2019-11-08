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

import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;


/**
 * Builder for operations to create rdf sources
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationBuilder extends AbstractRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    /**
     * Constructor.
     *
     * @param resourceId the internal identifier.
     */
    public CreateRdfSourceOperationBuilder(final String resourceId, final String interactionModel) {
        super(resourceId, interactionModel);
    }

    @Override
    public RdfSourceOperation build() {
        return new CreateRdfSourceOperation(this.resourceId, this.interactionModel, validateIncomingRdf(tripleStream));
    }

}
