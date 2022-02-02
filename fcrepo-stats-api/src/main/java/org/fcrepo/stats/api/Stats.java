/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

/**
 * @author dbernstein
 */
public interface Stats {
    /**
     * Retrieve the statistics for repository
     *
     * @param statsParams params the inform the resuslts
     * @return A stats results object
     */
    StatsResults getStatistics(StatsParameters statsParams);
}
