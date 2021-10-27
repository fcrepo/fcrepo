/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.Transaction;

/**
 * @author bbpennel
 * @author barmintor
 * @since Feb 21, 2014
 */
public interface Service<T> {
    /**
     * Test whether T exists at the given path in the
     * repository
     *
     * @param path the path
     * @param transaction the transaction
     * @return whether T exists at the given path
     */
    boolean exists(final Transaction transaction, final String path);
    /**
     * Retrieve an existing T instance by transaction and path
     *
     * @param path the path to the node
     * @param transaction the transaction
     * @return retrieved T
     */
    T find(final Transaction transaction, final String path);
    /**
     * Retrieve a T instance by transaction and path
     *
     * @param transaction the transaction
     * @param path the path to the node
     * @return retrieved T
     */
    T findOrCreate(final Transaction transaction, final String path);
}
