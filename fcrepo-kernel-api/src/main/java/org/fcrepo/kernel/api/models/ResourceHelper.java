/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Utility class interface for helper methods.
 * @author whikloj
 * @since 6.0.0
 */
public interface ResourceHelper {

    /**
     * Check if a resource exists.
     * @param transaction The current transaction
     * @param fedoraId The internal identifier
     * @param includeDeleted Whether to check for deleted resources too.
     * @return True if the identifier resolves to a resource.
     */
    public boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                     final boolean includeDeleted);

    /**
     * Is the resource a "ghost node". Ghost nodes are defined as a resource that does not exist, but whose URI is part
     * of the URI of another resource? For example:
     *
     * http://localhost/rest/a/b - does exist
     * http://localhost/rest/a - does not exist and is therefore a ghost node.
     *
     * @param transaction The transaction
     * @param resourceId Identifier of the resource
     * @return Whether the resource does not exist, but has
     */
    public boolean isGhostNode(final Transaction transaction, final FedoraId resourceId);
}
