/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

/**
 * An operation for creating a new version of a resource
 *
 * @author pwinckles
 */
public interface CreateVersionResourceOperation extends ResourceOperation {

    @Override
    default ResourceOperationType getType() {
        return UPDATE;
    }

}
