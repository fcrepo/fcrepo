/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;

import java.net.URI;
import java.util.List;

/**
 * An interface defining search index management operations
 *
 * @author dbernstein
 */
public interface SearchIndex {

    /**
     * Adds or updates the index with the resource header information.
     * @param transaction The externally generated transaction.
     * @param resourceHeaders The resource headers associated with the resource
     */
    void addUpdateIndex(Transaction transaction, ResourceHeaders resourceHeaders);

    /**
     * Adds or updates the index with the resource header information.
     * @param transaction The externally generated transaction.
     * @param resourceHeaders The resource headers associated with the resource
     * @param rdfTypes The RDF types associated with the resource
     */
    void addUpdateIndex(Transaction transaction, ResourceHeaders resourceHeaders,
                               List<URI> rdfTypes);

    /**
     * Removes indexed fields associated with the specified Fedora ID
     *
     * @param transaction The transaction
     * @param fedoraId    The Fedora ID
     */
    void removeFromIndex(Transaction transaction, FedoraId fedoraId);

    /**
     * Performs a search based on the parameters and returns the result.
     *
     * @param parameters The parameters defining the search
     * @return The result of the search
     */
    SearchResult doSearch(SearchParameters parameters) throws InvalidQueryException;

    /**
     * Remove all persistent state associated with the index.
     */
    void reset();

    /**
     * Commit the changes made in the transaction.
     *
     * @param tx The transaction .
     */
    void commitTransaction(final Transaction tx);

    /**
     * Rollback the changes in the transaction.
     *
     * @param tx The transaction.
     */
    void rollbackTransaction(final Transaction tx);

    /**
     * Clear all transactions in the search index.
     */
    void clearAllTransactions();
}
