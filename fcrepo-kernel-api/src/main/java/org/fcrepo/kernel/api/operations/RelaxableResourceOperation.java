/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import java.time.Instant;

/**
 * Operation involving a resource with relaxable server managed properties
 *
 * @author bbpennel
 */
public interface RelaxableResourceOperation extends ResourceOperation {
    /**
     * Get last modified by
     *
     * @return user that last modified the resource
     */
    String getLastModifiedBy();

    /**
     * Get created by
     *
     * @return user that created the resource
     */
    String getCreatedBy();

    /**
     * Get the timestamp the resource was last modified
     *
     * @return timestamp
     */
    Instant getLastModifiedDate();

    /**
     * Get the timestamp the resource was created
     *
     * @return timestamp
     */
    Instant getCreatedDate();
}
