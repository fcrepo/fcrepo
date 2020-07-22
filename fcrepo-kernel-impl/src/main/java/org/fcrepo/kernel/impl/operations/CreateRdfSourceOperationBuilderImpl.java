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

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;

/**
 * Builder for operations to create rdf sources
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationBuilderImpl extends AbstractRdfSourceOperationBuilder implements
        CreateRdfSourceOperationBuilder {

    private FedoraId parentId;

    private boolean archivalGroup = false;
    /**
     * Constructor.
     *
     * @param resourceId the internal identifier.
     * @param interactionModel interaction model
     */
    public CreateRdfSourceOperationBuilderImpl(final FedoraId resourceId, final String interactionModel) {
        super(resourceId, interactionModel);
    }

    @Override
    public CreateRdfSourceOperation build() {
        final var operation = new CreateRdfSourceOperationImpl(resourceId, interactionModel, tripleStream);
        operation.setParentId(parentId);
        operation.setUserPrincipal(userPrincipal);
        operation.setCreatedBy(createdBy);
        operation.setCreatedDate(createdDate);
        operation.setLastModifiedBy(lastModifiedBy);
        operation.setLastModifiedDate(lastModifiedDate);
        operation.setArchivalGroup(archivalGroup);
        return operation;
    }

    @Override
    public CreateRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        super.userPrincipal(userPrincipal);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder triples(final RdfStream triples) {
        super.triples(triples);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder parentId(final FedoraId parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder relaxedProperties(final Model model) {
        super.relaxedProperties(model);
        return this;
    }

    @Override
    public CreateRdfSourceOperationBuilder archivalGroup(final boolean flag) {
        this.archivalGroup = flag;
        return this;
    }

}
