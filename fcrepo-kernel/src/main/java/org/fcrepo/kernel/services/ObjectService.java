/**
 * Copyright 2013 DuraSpace, Inc.
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

/*import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;*/
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraObject;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Service for creating and retrieving FedoraObjects without using the JCR API.
 * 
 * @author cbeer
 * @date Feb 11, 2013
 */
@Component
public class ObjectService extends RepositoryService implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(ObjectService.class);
    private static final String DEFAULT_NT = "fedora:baseObject";

    /**
     * @param session A JCR Session
     * @param path The path to use to create the object
     * @return The created object
     * @throws RepositoryException
     */
    public FedoraObject createObject(final Session session, final String path)
        throws RepositoryException {
        return createObject(session, path, DEFAULT_NT);
    }

    /**
     * @param session     A JCR Session
     * @param path        The path to use to create the object
     * @param primaryType The primary node type
     * @return The created object
     * @throws RepositoryException
     * @author Stefano Cossu
     */
    public FedoraObject createObject(final Session session, final String path, final String primaryType)
        throws RepositoryException {
        if (null == primaryType || primaryType.isEmpty()) {
            return new FedoraObject(session, path, DEFAULT_NT);
        } else {
            return new FedoraObject(session, path, primaryType);
        }
    }

    /**
     * @param path
     * @return The JCR node behind the FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public Node getObjectNode(final Session session, final String path)
        throws RepositoryException {
        return session.getNode(path);
    }

    /**
     * @param path
     * @param session
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public FedoraObject getObject(final Session session, final String path)
        throws RepositoryException {
        LOGGER.trace("Executing getObject() with path: {}", path);
        return new FedoraObject(getObjectNode(session, path));
    }

}
