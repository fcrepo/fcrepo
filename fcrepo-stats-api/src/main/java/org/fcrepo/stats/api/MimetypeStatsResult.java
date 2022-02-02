/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dbernstein
 */
public class MimetypeStatsResult extends BinaryStatsResult {

    @JsonProperty("mime_type")
    private String mimetype;

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(final String mimetype) {
        this.mimetype = mimetype;
    }
}
