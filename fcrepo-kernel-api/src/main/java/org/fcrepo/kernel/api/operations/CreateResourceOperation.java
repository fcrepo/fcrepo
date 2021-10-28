/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * Operation for creating a resource
 *
 * @author bbpennel
 */
public interface CreateResourceOperation extends ResourceOperation {

    /**
     * Get the identifier of the parent of the resource being created
     *
     * @return identifer of parent
     */
    FedoraId getParentId();

    /**
     * Get the interaction model of the resource being created
     *
     * @return interaction model
     */
    String getInteractionModel();

    @Override
    public default ResourceOperationType getType() {
        return CREATE;
    }

    /**
     * A flag indicating whether or the new resource should be created as an archival group.
     * @return true if archival group
     */
    boolean isArchivalGroup();
}
