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

import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;

import java.time.Instant;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;

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

    protected AbstractRdfSourceOperationBuilder(final FedoraId rescId, final String model) {
        resourceId = rescId;
        interactionModel = model;
    }

    @Override
    public RdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        this.tripleStream = triples;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder relaxedProperties(final Model model) {
        // Has no affect if the server is not in relaxed mode
        if (model != null && isRelaxed()) {
            final var resc = model.getResource(resourceId.getResourceId());
            final var createdDateVal = getPropertyAsInstant(resc, CREATED_DATE);
            if (createdDateVal != null) {
                this.createdDate = createdDateVal;
            }
            final var createdByVal = getStringProperty(resc, CREATED_BY);
            if (createdByVal != null) {
                this.createdBy = createdByVal;
            }
            final var modifiedDate = getPropertyAsInstant(resc, LAST_MODIFIED_DATE);
            if (modifiedDate != null) {
                this.lastModifiedDate = modifiedDate;
            }
            final var modifiedBy = getStringProperty(resc, LAST_MODIFIED_BY);
            if (modifiedBy != null) {
                this.lastModifiedBy = modifiedBy;
            }
        }

        return this;
    }

    private static String getStringProperty(final Resource resc, final Property property) {
        if (resc.hasProperty(property)) {
            return resc.getProperty(property).getString();
        }

        return null;
    }

    private static Instant getPropertyAsInstant(final Resource resc, final Property property) {
        if (resc.hasProperty(property)) {
            final var propObj = resc.getProperty(property).getObject();

            if (propObj.isLiteral()) {
                final var literalValue = propObj.asLiteral().getValue();
                if (literalValue instanceof Date) {
                    return ((Date) literalValue).toInstant();
                }
            }
        }

        return null;
    }

    private static boolean isRelaxed() {
        return "relaxed".equals(System.getProperty(SERVER_MANAGED_PROPERTIES_MODE));
    }
}
