/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.OVERWRITE_TOMBSTONE;

import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;

/**
 * OverwriteTombstoneOperation -- a special case of a Create operation
 *
 * @author mikejritter
 */
public interface OverwriteTombstoneOperation extends CreateResourceOperation {

    @Override
    default ResourceOperationType getType() {
        return OVERWRITE_TOMBSTONE;
    }

}
