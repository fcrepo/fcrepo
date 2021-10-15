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
package org.fcrepo.kernel.impl.operations;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RelaxableResourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import java.time.Instant;

import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.checkTripleForDisallowed;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedDate;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedDate;

/**
 * Abstract builder for constructing relaxable resource operations
 * @author bbpennel
 */
public abstract class AbstractRelaxableResourceOperationBuilder implements RelaxableResourceOperationBuilder {

    /**
     * String of the resource ID.
     */
    protected final FedoraId resourceId;

    protected String lastModifiedBy;

    protected String createdBy;

    protected Instant lastModifiedDate;

    protected Instant createdDate;

    protected ServerManagedPropsMode serverManagedPropsMode;

    protected Transaction transaction;

    protected AbstractRelaxableResourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                        final ServerManagedPropsMode serverManagedPropsMode) {
        this.transaction = transaction;
        this.resourceId = rescId;
        this.serverManagedPropsMode = serverManagedPropsMode;
    }

    @Override
    public RelaxableResourceOperationBuilder relaxedProperties(final Model model) {
        // Has no affect if the server is not in relaxed mode
        if (model != null && serverManagedPropsMode == ServerManagedPropsMode.RELAXED) {
            final var resc = model.getResource(resourceId.getResourceId());

            final var createdDateVal = getCreatedDate(resc);
            if (createdDateVal != null) {
                this.createdDate = createdDateVal.toInstant();
            }
            final var createdByVal = getCreatedBy(resc);
            if (createdByVal != null) {
                this.createdBy = createdByVal;
            }
            final var modifiedDate = getModifiedDate(resc);
            if (modifiedDate != null) {
                this.lastModifiedDate = modifiedDate.toInstant();
            }
            final var modifiedBy = getModifiedBy(resc);
            if (modifiedBy != null) {
                this.lastModifiedBy = modifiedBy;
            }
        }

        return this;
    }
}
