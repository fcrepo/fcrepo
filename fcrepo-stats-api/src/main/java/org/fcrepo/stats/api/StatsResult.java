/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data object representing a statistics result.
 * @author dbernstein
 */
public class StatsResult {

    @JsonProperty("resource_count")
    private Long resourceCount = 0l;

    public void setResourceCount(final Long resourceCount) {
        this.resourceCount = resourceCount;
    }

    public Long getResourceCount() {
        return resourceCount;
    }
}
