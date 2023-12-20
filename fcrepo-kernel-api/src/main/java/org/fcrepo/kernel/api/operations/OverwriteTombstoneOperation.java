/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.OVERWRITE_TOMBSTONE;

/**
 * Operation for overwriting the tombstone of an existing resource
 *
 * @author mikejritter
 */
public interface OverwriteTombstoneOperation extends CreateResourceOperation {

    @Override
    default ResourceOperationType getType() {
        return OVERWRITE_TOMBSTONE;
    }

}
