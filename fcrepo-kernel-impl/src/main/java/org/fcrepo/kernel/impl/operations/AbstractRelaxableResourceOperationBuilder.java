/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RelaxableResourceOperationBuilder;

import java.time.Instant;

import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getCreatedDate;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedBy;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.getModifiedDate;

/**
 * Abstract builder for constructing relaxable resource operations
 * @author bbpennel
 */
public abstract class AbstractRelaxableResourceOperationBuilder extends AbstractResourceOperationBuilder
                                                                implements RelaxableResourceOperationBuilder {
    protected String lastModifiedBy;

    protected String createdBy;

    protected Instant lastModifiedDate;

    protected Instant createdDate;

    protected ServerManagedPropsMode serverManagedPropsMode;

    protected AbstractRelaxableResourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                        final ServerManagedPropsMode serverManagedPropsMode) {
        super(transaction, rescId);
        this.serverManagedPropsMode = serverManagedPropsMode;
    }

    @Override
    public RelaxableResourceOperationBuilder relaxedProperties(final Model model) {
        // Has no affect if the server is not in relaxed mode
        if (model != null && serverManagedPropsMode == ServerManagedPropsMode.RELAXED) {
            final var resc = model.getResource(rescId.getResourceId());

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
