/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data object representing an aggregated stats result.
 *
 * @author dbernstein
 */
public class AggregatedRepositoryStatsResults extends RepositoryStatsResult {
    @JsonProperty("binaries")
    private RepositoryStatsByMimeTypeResults binaries;

    @JsonProperty("all_resources")
    private RepositoryStatsByRdfTypeResults allResources;

    public RepositoryStatsByMimeTypeResults getBinaries() {
        return binaries;
    }

    public void setBinaries(final RepositoryStatsByMimeTypeResults binaries) {
        this.binaries = binaries;
    }

    public RepositoryStatsByRdfTypeResults getAllResources() {
        return allResources;
    }

    public void setAllResources(final RepositoryStatsByRdfTypeResults allResources) {
        this.allResources = allResources;
    }
}
