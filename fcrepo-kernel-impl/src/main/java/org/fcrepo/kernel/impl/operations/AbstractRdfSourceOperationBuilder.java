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

import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.checkTripleForDisallowed;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedDate;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedDate;

import java.time.Instant;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import org.apache.jena.rdf.model.Model;

/**
 * Abstract builder for interacting with an Rdf Source Operation Builder
 * @author bseeger
 */
public abstract class AbstractRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    /**
     * Holds the stream of user's triples.
     */
    protected RdfStream tripleStream;

    /**
     * String of the resource ID.
     */
    protected final FedoraId resourceId;

    /**
     * The interaction model of this resource, null in case of update.
     */
    protected final String interactionModel;

    /**
     * Principal of the user performing the operation
     */
    protected String userPrincipal;

    protected String lastModifiedBy;

    protected String createdBy;

    protected Instant lastModifiedDate;

    protected Instant createdDate;

    protected ServerManagedPropsMode serverManagedPropsMode;

    protected AbstractRdfSourceOperationBuilder(final FedoraId rescId,
                                                final String model,
                                                final ServerManagedPropsMode serverManagedPropsMode) {
        resourceId = rescId;
        interactionModel = model;
        this.serverManagedPropsMode = serverManagedPropsMode;
    }

    @Override
    public RdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        // Filter out server managed properties, they should only matter to the relaxedProperties method.
        this.tripleStream = new DefaultRdfStream(triples.topic(), triples.filter(t -> {
            try {
                checkTripleForDisallowed(t);
            } catch (final Exception e) {
                return false;
            }
            return true;
        }));
        return this;
    }

    @Override
    public RdfSourceOperationBuilder relaxedProperties(final Model model) {
        // Has no affect if the server is not in relaxed mode
        if (model != null && serverManagedPropsMode == ServerManagedPropsMode.RELAXED) {
            final var resc = model.getResource(resourceId.getResourceId());

            final var createdDateVal = getCreatedDate(model, resc);
            if (createdDateVal != null) {
                this.createdDate = createdDateVal.toInstant();
            }
            final var createdByVal = getCreatedBy(model, resc);
            if (createdByVal != null) {
                this.createdBy = createdByVal;
            }
            final var modifiedDate = getModifiedDate(model, resc);
            if (modifiedDate != null) {
                this.lastModifiedDate = modifiedDate.toInstant();
            }
            final var modifiedBy = getModifiedBy(model, resc);
            if (modifiedBy != null) {
                this.lastModifiedBy = modifiedBy;
            }
        }

        return this;
    }
}
