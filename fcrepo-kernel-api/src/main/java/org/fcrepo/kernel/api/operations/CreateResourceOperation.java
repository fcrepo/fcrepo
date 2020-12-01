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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * Operation for creating a resource
 *
 * @author bbpennel
 */
public interface CreateResourceOperation extends ResourceOperation {

    /**
     * Get the identifier of the parent of the resource being created
     *
     * @return identifer of parent
     */
    FedoraId getParentId();

    /**
     * Get the interaction model of the resource being created
     *
     * @return interaction model
     */
    String getInteractionModel();

    @Override
    public default ResourceOperationType getType() {
        return CREATE;
    }

    /**
     * A flag indicating whether or the new resource should be created as an archival group.
     * @return true if archival group
     */
    boolean isArchivalGroup();
}
