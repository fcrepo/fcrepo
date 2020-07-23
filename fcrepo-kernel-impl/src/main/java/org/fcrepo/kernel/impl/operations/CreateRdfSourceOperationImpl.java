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

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;

/**
 * Operation to create an RDF source.
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationImpl extends AbstractRdfSourceOperation implements CreateRdfSourceOperation {

    private FedoraId parentId;

    /**
     * The interaction model
     */
    private String interactionModel;

    private boolean archivalGroup = false;

    /**
     * Constructor for creation operation
     *
     * @param rescId the internal identifier.
     * @param interactionModel interaction model for the resource
     * @param triples triples stream for the resource
     */
    protected CreateRdfSourceOperationImpl(final FedoraId rescId, final String interactionModel,
                                           final RdfStream triples) {
        super(rescId, triples);
        this.interactionModel = interactionModel;
    }

    @Override
    public String getInteractionModel() {
        return interactionModel;
    }

    @Override
    public boolean isArchivalGroup() {
        return this.archivalGroup;
    }

    @Override
    public FedoraId getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(final FedoraId parentId) {
        this.parentId = parentId;
    }

    /**
     *
     * @param flag flag indicating whether resource is an Archival Group
     */
    public void setArchivalGroup(final boolean flag) {
        this.archivalGroup = flag;
    }


}
