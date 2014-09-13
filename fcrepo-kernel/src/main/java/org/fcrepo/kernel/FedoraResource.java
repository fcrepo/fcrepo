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
package org.fcrepo.kernel;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.kernel.resource.Container;

/**
 * @author ajs6f
 * @since Jan 10, 2014
 */
public interface FedoraResource extends Container<Node> {

    /**
     * Does the resource have a jcr:content child node?
     * @return has content
     * @throws RepositoryException
     */
    boolean hasContent() throws RepositoryException;

    /**
     * Tag the current version of the Node with a version label that
     * can be retrieved by name later.
     *
     * @param label
     * @throws RepositoryException
     */
    void addVersionLabel(final String label) throws RepositoryException;

    /**
     * Get the JCR Base version for the node
     * 
     * @return base version
     * @throws RepositoryException
     */
    public Version getBaseVersion() throws RepositoryException;

    /**
     * Get JCR VersionHistory for the node.
     *
     * @return version history
     * @throws RepositoryException
     */
    public VersionHistory getVersionHistory() throws RepositoryException;

}