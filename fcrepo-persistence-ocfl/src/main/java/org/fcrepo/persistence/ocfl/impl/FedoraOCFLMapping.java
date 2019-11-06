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

/**
 * A data structure that linking parent fedora resource identifiers to its corresponding OCFL Object Id.
 *
 * @author dbernstein
 */
public class FedoraOCFLMapping {
    private String parentFedoraResourceId;
    private String ocflObjectId;

    /**
     * Default constructor
     * @param parentFedoraResourceId The parent fedora resource identifier
     * @param ocflObjectId The OCFL Object identitifer
     */
    public FedoraOCFLMapping(final String parentFedoraResourceId, final String ocflObjectId){
        this.parentFedoraResourceId = parentFedoraResourceId;
        this.ocflObjectId = ocflObjectId;
    }

    /**
     * Retrieve the parent fedora resource identifier associated with the OCFL Object Id.
     * It is a "parent" identifier because it refers to the Archival Group identifier for
     * the fedora resource associated with this mapping in the case that the resource was
     * part of an Archival Group.  In the case of binary descriptive metadata, this identifer
     * will correspond to "root" or "parent" of the resource.
     * @see {@link org.fcrepo.persistence.ocfl.FedoraToOCFLObjectIndex}
     * @return the fedora resource identifier
     */
    public String getParentFedoraResourceId() {
        return parentFedoraResourceId;
    }

    /**
     * Retrieve the OCFL object identifier associated with the Fedora resource
     * @return the ocfl object identifier
     */
    public String getOcflObjectId() {
        return ocflObjectId;
    }
}
