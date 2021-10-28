/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.api;


/**
 * An interface representing index rebuilding capabilities. Any index state that can be derived from the underlying OCFL
 * repository must be regenerated by the implementing class.
 *
 * repository
 * @author dbernstein
 * @since 6.0.0
 */
public interface IndexBuilder {

    /**
     * Rebuilds the index only when the existing index is not populated.
     */
    void rebuildIfNecessary();

}

