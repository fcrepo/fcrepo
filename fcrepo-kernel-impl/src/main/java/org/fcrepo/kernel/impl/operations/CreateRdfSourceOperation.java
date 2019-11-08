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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;


/**
 * Operation to create an RDF source.
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperation extends AbstractRdfSourceOperation implements CreateResourceOperation {

    private String interactionModel;

    protected CreateRdfSourceOperation(final String rescId, final RdfStream triples) {
        super(rescId, triples);
    }

    @Override
    public ResourceOperationType getType() {
        return CREATE;
    }

    @Override
    public String getInteractionModel() {
        return interactionModel;
    }

    @Override
    public void setInteractionModel(final String interactionModel) {
        this.interactionModel = interactionModel;
    }
}
