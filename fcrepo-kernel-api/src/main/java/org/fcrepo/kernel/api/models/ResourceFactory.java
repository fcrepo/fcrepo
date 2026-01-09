/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import java.util.stream.Stream;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Interface to a factory to instantiate FedoraResources
 *
 * @author whikloj
 * @since 2019-09-23
 */
public interface ResourceFactory {

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transaction The transaction associated with this request or null if not in a transaction.
     * @param fedoraID The identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final Transaction transaction, final FedoraId fedoraID)
            throws PathNotFoundException;

    /**
     * Get a resource as a particular type
     *
     * @param <T> type for the resource
     * @param transaction The transaction associated with this request or null
     * @param fedoraID The identifier for the resource.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final Transaction transaction, final FedoraId fedoraID,
                                                    final Class<T> clazz) throws PathNotFoundException;

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transaction The transaction associated with this request or null if not in a transaction.
     * @param headers     The resource headers to use.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    FedoraResource getResource(final Transaction transaction,
                               final ResourceHeaders headers) throws PathNotFoundException;

    /**
     * Get the containing resource (if exists).
     * @param transaction The current transaction
     * @param resourceId The internal identifier
     * @return The containing resource or null if none.
     */
    public FedoraResource getContainer(final Transaction transaction, final FedoraId resourceId);

    /**
     * Get immediate children of the resource
     * @param transaction The transaction
     * @param resourceId Identifier of the resource
     * @return Stream of child resources
     */
    public Stream<FedoraResource> getChildren(final Transaction transaction, final FedoraId resourceId);
}
