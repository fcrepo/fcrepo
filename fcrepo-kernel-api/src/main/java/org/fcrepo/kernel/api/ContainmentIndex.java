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
package org.fcrepo.kernel.api;

import java.util.stream.Stream;

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * An interface for retrieving resource IDs by their containment relationships.
 *
 * @author dbernstein
 * @since 6.0.0
 */
public interface ContainmentIndex {

    /**
     * Return a stream of fedora identifiers contained by the specified fedora resource.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param fedoraResource The containing fedora resource
     * @return A stream of contained identifiers
     */
    Stream<String> getContainedBy(Transaction tx, FedoraResource fedoraResource);

    /**
     * Remove a contained by relation between the child resource and its parent.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param parent The containing fedora resource
     * @param child The contained fedora resource
     */
    void removeContainedBy(final Transaction tx, final FedoraResource parent, final FedoraResource child);

    /**
     * Add a contained by relation between the child resource and its parent.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param parent The containing fedora resource
     * @param child The contained fedora resource
     */
    void addContainedBy(final Transaction tx, final FedoraResource parent, final FedoraResource child);

    /**
     * Add a contained by relation between the child resource and its parent.
     *
     * @param txID The transaction ID.  If no transaction, null is okay.
     * @param parentID The containing fedora resource ID.
     * @param childID The contained fedora resource ID.
     */
    void addContainedBy(final String txID, final String parentID, final String childID);

    /**
     * Commit the changes made in the transaction.
     * @param tx The transaction.
     */
    void commitTransaction(final Transaction tx);

    /**
     * Rollback the containment index changes in the transaction.
     * @param tx The transaction.
     */
    void rollbackTransaction(final Transaction tx);

    /**
     * Check if the resourceID exists in the containment index. Which should mean it exists.
     *
     * @param txID The transaction ID or null if not transaction.
     * @param resourceID The resource ID.
     * @return True if it is in the index.
     */
    boolean resourceExists(final String txID, final String resourceID);
}
