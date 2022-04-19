/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

/**
 * An interface that provides access to repository stats.
 * @author dbernstein
 */
public interface RepositoryStats {
    /**
     * Retrieve binaries stats broken out by mime type
     *
     * @param statsParams params that inform the results
     * @return A stats results object
     */
    RepositoryStatsByMimeTypeResults getByMimeTypes(final RepositoryStatsParameters statsParams);

    /**
     * Retrieve resource stats broken out by RDF type
     * @param statsParams params that inform the results
     * @return A stats results object
     */
    RepositoryStatsByRdfTypeResults getByRdfType(final RepositoryStatsParameters statsParams);

    /**
     * Retrieve a count of all resources in the repository
     * @param statsParams params that inform the results
     * @return A stats results object
     */
     RepositoryStatsResult getResourceCount(final RepositoryStatsParameters statsParams);
}
