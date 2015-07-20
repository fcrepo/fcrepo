/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.api.services;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * @author bbpennel
 * @author barmintor
 * @since Feb 21, 2014
 */
public interface Service<T> {
    /**
     * Test whether T exists at the given path in the
     * repository
     *
     * @param path the path
     * @param session the session
     * @return whether T exists at the given path
     */
    public boolean exists(final Session session, final String path);
    /**
     * Retrieve an existing T instance by session and path
     *
     * @param path jcr path to the node
     * @param session the session
     * @return retrieved T
     */
    public T find(final Session session, final String path);
    /**
     * Retrieve a T instance by session and path
     *
     * @param session the session
     * @param path jcr path to the node
     * @return retrieved T
     */
    public T findOrCreate(final Session session, final String path);
    /**
     * Retrieve a T instance from a node
     *
     * @param node the node
     * @return node as T
     */
    public T cast(Node node);
}
