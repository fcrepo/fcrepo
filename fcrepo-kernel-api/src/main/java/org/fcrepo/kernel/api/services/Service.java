/*
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

/**
 * @author bbpennel
 * @author barmintor
 * @since Feb 21, 2014
 */
public interface Service<ResourceType, AccessType> {
    /**
     * Test whether a resource type exists at the given path in the
     * repository
     *
     * @param path the path
     * @param access the session
     * @return whether resource exists at the given path
     */
    public boolean exists(final AccessType access, final String path);
    /**
     * Retrieve an existing T instance by access and path
     *
     * @param path jcr path to the node
     * @param access the session
     * @return retrieved resource
     */
    public ResourceType find(final AccessType access, final String path);
    /**
     * Retrieve a T instance by session and path
     *
     * @param access the session
     * @param path jcr path to the node
     * @return retrieved resource
     */
    public ResourceType findOrCreate(final AccessType access, final String path);
    /**
     * Retrieve a resource instance from a node
     *
     * @param node the node
     * @return node as ResourceType
     */
    public ResourceType cast(Node node);
}
