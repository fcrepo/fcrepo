/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api;

/**
 * Status of repository initialization process
 *
 * @author bbpennel
 */
public interface RepositoryInitializationStatus {
    /**
     * Check if repository initialization is complete
     *
     * @return true if initialization is complete
     */
    boolean isInitializationComplete();

    /**
     * Set the initialization complete status
     *
     * @param complete true if initialization is complete
     */
    void setInitializationComplete(boolean complete);
}
