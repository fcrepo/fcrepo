/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dbernstein
 */
public class StatsResults {
    @JsonProperty
    private List<Map<String, Object>> mimetypes;

    @JsonProperty
    private Long resourceCount = -1l;

    @JsonProperty
    private Long binaryResourceCount = -1l;

    @JsonProperty
    private Long binaryResourceBytes = -1l;

    public List<Map<String, Object>> getMimetypes() {
        return mimetypes;
    }

    public void setResourceCount(final Long resourceCount) {
        this.resourceCount = resourceCount;
    }

    public Long getResourceCount() {
        return resourceCount;
    }

    public Long getBinaryResourceCount() {
        return binaryResourceCount;
    }

    public void setBinaryResourceCount(final Long binaryResourceCount) {
        this.binaryResourceCount = binaryResourceCount;
    }

    public Long getBinaryResourceBytes() {
        return binaryResourceBytes;
    }

    public void setBinaryResourceBytes(final Long binaryResourceBytes) {
        this.binaryResourceBytes = binaryResourceBytes;
    }

}
