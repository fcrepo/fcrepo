/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  A data object representing a statistics result a specified mime type.
 * @author dbernstein
 */
public class MimeTypeStatsResult extends BinaryStatsResult {

    @JsonProperty("mime_type")
    private String mimeType;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }
}
