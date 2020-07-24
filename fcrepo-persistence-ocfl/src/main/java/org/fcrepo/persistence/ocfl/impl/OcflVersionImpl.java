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

import org.fcrepo.persistence.ocfl.api.OcflVersion;

import java.time.Instant;

/**
 * Default OcflVersion impl
 *
 * @author pwinckles
 */
public class OcflVersionImpl implements OcflVersion {

    private String ocflObjectId;
    private String ocflVersionId;
    private Instant created;
    private String createdBy;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOcflObjectId() {
        return ocflObjectId;
    }

    /**
     * @param ocflObjectId OCFL object id
     * @return this object for chaining
     */
    public OcflVersionImpl setOcflObjectId(final String ocflObjectId) {
        this.ocflObjectId = ocflObjectId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOcflVersionId() {
        return ocflVersionId;
    }

    /**
     * @param ocflVersionId OCFL version id
     * @return this object for chaining
     */
    public OcflVersionImpl setOcflVersionId(final String ocflVersionId) {
        this.ocflVersionId = ocflVersionId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant getCreated() {
        return created;
    }

    /**
     * @param created Instant version was created
     * @return this object for chaining
     */
    public OcflVersionImpl setCreated(final Instant created) {
        this.created = created;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy who created the version
     * @return this object for chaining
     */
    public OcflVersionImpl setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

}
