/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A data object representing a statistics result.
 * @author dbernstein
 */
public class RepositoryStatsResult {

    private Long resourceCount = 0L;

    public void setResourceCount(final Long resourceCount) {
        this.resourceCount = resourceCount;
    }

    @JsonProperty("resource_count")
    public Long getResourceCount() {
        return resourceCount;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this).toString();
    }
}
