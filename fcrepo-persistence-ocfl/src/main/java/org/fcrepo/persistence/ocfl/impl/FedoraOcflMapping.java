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
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.util.Objects;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * A mapping that links the parent fedora resource to its corresponding OCFL object.
 *
 * @author dbernstein
 */
public class FedoraOcflMapping {

    private final FedoraId rootObjectIdentifier;
    private final String ocflObjectId;

    /**
     * Default constructor
     * @param rootObjectIdentifier The fedora root object resource identifier
     * @param ocflObjectId The OCFL Object identitifer
     */
    public FedoraOcflMapping(final FedoraId rootObjectIdentifier, final String ocflObjectId) {
        this.rootObjectIdentifier = rootObjectIdentifier;
        this.ocflObjectId = ocflObjectId;
    }

    /**
     * The id for the fedora resource which represents this ocfl object
     * @return the fedora root object identifier
     */
    public FedoraId getRootObjectIdentifier() {
        return rootObjectIdentifier;
    }

    /**
     * Retrieve the OCFL object identifier associated with the Fedora resource
     * @return the ocfl object identifier
     */
    public String getOcflObjectId() {
        return ocflObjectId;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FedoraOcflMapping that = (FedoraOcflMapping) o;
        return rootObjectIdentifier.equals(that.rootObjectIdentifier) &&
                ocflObjectId.equals(that.ocflObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootObjectIdentifier, ocflObjectId);
    }

}
