/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.services;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraObject;

/**
 * Service for creating and retrieving FedoraObjects
 *
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface ObjectService extends Service {

    /**
     * @param session
     * @param path The path to use to create the object
     * @return The created object
     * @throws RepositoryException
     */
    FedoraObject createObject(Session session, String path) throws RepositoryException;

    /**
     * @param path
     * @return The node behind the FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    Node getObjectNode(Session session, String path) throws RepositoryException;

    /**
     * @param path
     * @param session
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    FedoraObject getObject(Session session, String path) throws RepositoryException;

}