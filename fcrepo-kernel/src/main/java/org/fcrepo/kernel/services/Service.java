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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author bbpennel
 * @since Feb 21, 2014
 */
public interface Service {

    /**
     * Set the repository to back this RepositoryService
     *
     * @param repository
     */
    void setRepository(Repository repository);

    /**
     * Test whether a datastream or object exists at the given path in the
     * repository
     *
     * @param path
     * @return whether a datastream or object exists at the given path
     * @throws RepositoryException
     */
    public boolean exists(final Session session, final String path) throws RepositoryException;

    /**
     * Validate resource path for unregistered namespace prefixes
     *
     * @param session the JCR session to use
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public void validatePath(final Session session, final String path) throws RepositoryException;

}