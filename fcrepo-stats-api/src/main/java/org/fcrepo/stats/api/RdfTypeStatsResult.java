/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  A data object representing a statistics result a specified resource type
 * @author dbernstein
 */
public class RdfTypeStatsResult extends BinaryStatsResult {

    @JsonProperty("resource_type")
    private String resourceType;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }
}
