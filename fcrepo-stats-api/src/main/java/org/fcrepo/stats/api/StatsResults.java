/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dbernstein
 */
public class StatsResults {
    @JsonProperty("mime_types")
    private List<MimetypeStatsResult> mimetypes;

    @JsonProperty
    private StatsResult all;

    @JsonProperty
    private BinaryStatsResult binaries;

    public List<MimetypeStatsResult> getMimetypes() {
        return mimetypes;
    }

    public void setMimetypes(final List<MimetypeStatsResult> mimetypes) {
        this.mimetypes = mimetypes;
    }

    public StatsResult getAll() {
        return all;
    }

    public void setAll(final StatsResult all) {
        this.all = all;
    }

    public BinaryStatsResult getBinaries() {
        return binaries;
    }

    public void setBinaries(final BinaryStatsResult binaries) {
        this.binaries = binaries;
    }
}
