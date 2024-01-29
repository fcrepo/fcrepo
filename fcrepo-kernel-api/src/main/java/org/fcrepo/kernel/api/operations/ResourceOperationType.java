/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;


/**
 * Specifies the type of modification action represented by a resource operation.
 *
 * @author bbpennel
 */
public enum ResourceOperationType {
    UPDATE, UPDATE_HEADERS, CREATE, DELETE, PURGE, FOLLOW, REINDEX, OVERWRITE_TOMBSTONE
}
