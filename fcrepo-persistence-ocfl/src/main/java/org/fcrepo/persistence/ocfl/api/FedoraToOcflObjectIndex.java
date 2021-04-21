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
package org.fcrepo.persistence.ocfl.api;

import javax.annotation.Nonnull;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.impl.FedoraOcflMapping;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public interface FedoraToOcflObjectIndex {

    /**
     * Retrieve identification information for the OCFL object which either contains, or is identified by,
     * the provided fedora resource id. In other words the method will find the closest resource that is persisted
     * as an OCFL object and returns its identifiers.
     *
     * If you pass fedora identifier that is not part of an archival group such as
     * "my/fedora/binary/fcr:metadata"  the  fedora resource returned in the mapping will be "my/fedora/binary".
     *
     * Contrast this  with an Archival Group example:  if you pass in "my/archival-group/binary/fcr:metadata" the
     * resource returned in the mapping would be "my/archival-group".
     *
     * @param session the current session, or null for read-only.
     * @param fedoraResourceIdentifier the fedora resource identifier
     *
     * @return the mapping
     * @throws FedoraOcflMappingNotFoundException when no mapping exists for the specified identifier.
     */
    FedoraOcflMapping getMapping(final Transaction session, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException;

    /**
     * Retrieve identification information for the OCFL object which either contains, or is identified by,
     * the provided fedora resource id. In other words the method will find the closest resource that is persisted
     * as an OCFL object and returns its identifiers.
     *
     * If you pass fedora identifier that is not part of an archival group such as
     * "my/fedora/binary/fcr:metadata"  the  fedora resource returned in the mapping will be "my/fedora/binary".
     *
     * Contrast this  with an Archival Group example:  if you pass in "my/archival-group/binary/fcr:metadata" the
     * resource returned in the mapping would be "my/archival-group".
     *
     * NOTE: This method ALWAYS uses the transaction tables, and so will perform slower queries. Use only as necessary.
     *
     * @param session the current session, or null for read-only.
     * @param fedoraResourceIdentifier the fedora resource identifier
     *
     * @return the mapping
     * @throws FedoraOcflMappingNotFoundException when no mapping exists for the specified identifier.
     */
    FedoraOcflMapping getMappingInternal(final Transaction session, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException;

    /**
     * Adds a mapping to the index
     *
     * @param session the current session.
     * @param fedoraResourceIdentifier The fedora resource
     * @param fedoraRootObjectIdentifier   The identifier of the root fedora object resource
     * @param ocflObjectId             The ocfl object id
     * @return  The newly created mapping
     */
    FedoraOcflMapping addMapping(@Nonnull Transaction session, final FedoraId fedoraResourceIdentifier,
                                 final FedoraId fedoraRootObjectIdentifier, final String ocflObjectId);

    /**
     * Removes a mapping
     *
     * @param session the current session.
     * @param fedoraResourceIdentifier The fedora resource to remove the mapping for
     */
    void removeMapping(@Nonnull final Transaction session, final FedoraId fedoraResourceIdentifier);

    /**
     * Remove all persistent state associated with the index.
     */
    void reset();

    /**
     * Commit mapping changes for the session.
     *
     * @param session the session to commit.
     */
    void commit(@Nonnull final Transaction session);

    /**
     * Rollback mapping changes for the session.
     *
     * @param session the session to rollback.
     */
    void rollback(@Nonnull final Transaction session);

}

