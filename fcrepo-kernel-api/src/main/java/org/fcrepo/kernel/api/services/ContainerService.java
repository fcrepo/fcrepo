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
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.Container;

/**
 * Service for creating and retrieving {@link org.fcrepo.kernel.api.models.Container}
 *
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface ContainerService extends Service<Container> {

    /**
     * Find or create a container node using the provided interaction model.
     *
     * @param session the session
     * @param path the path
     * @param interactionModel interaction model for the container node
     * @return Container object for the given path.
     */
    public Container findOrCreate(final FedoraSession session, final String path, final String interactionModel);
}
