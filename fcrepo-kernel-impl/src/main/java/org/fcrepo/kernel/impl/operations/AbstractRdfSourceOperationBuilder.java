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
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;

public abstract class AbstractRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    /**
     * Holds the stream of user's triples.
     */
    protected RdfStream tripleStream;

    /**
     * String of the resource ID.
     */
    protected final String resourceId;

    /**
     * The interaction model of this resource, null in case of update.
     */
    protected final String interactionModel;

    protected AbstractRdfSourceOperationBuilder(final String rescId, final String model) {
        resourceId = rescId;
        interactionModel = model;
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        this.tripleStream = triples;
        return this;
    }

}
