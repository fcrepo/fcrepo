/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;


/**
 * A builder for constructing resource operations
 *
 * @author bbpennel
 */
public interface ResourceOperationBuilder {

    /**
     * Set the principal for the user performing the operation
     *
     * @param userPrincipal user principal
     * @return this builder
     */
    ResourceOperationBuilder userPrincipal(String userPrincipal);

    /**
     * Build the ResourceOperation constructed by this builder
     *
     * @return the constructed operation
     */
    ResourceOperation build();
}
