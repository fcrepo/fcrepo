/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  A data object representing a statistics result for binary objects
 * @author dbernstein
 */
public class BinaryStatsResult extends StatsResult {

    @JsonProperty("byte_count")
    private Long byteCount = -1l;

    public Long getByteCount() {
        return byteCount;
    }

    public void setByteCount(final Long byteCount) {
        this.byteCount = byteCount;
    }

}
