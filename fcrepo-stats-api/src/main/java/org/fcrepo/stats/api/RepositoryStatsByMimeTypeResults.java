/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A repository stats object for binary results by mime type.
 * @author dbernstein
 */
public class RepositoryStatsByMimeTypeResults {

    @JsonProperty("mime_types")
    private List<MimeTypeStatsResult> mimeTypes = new ArrayList<>();

    public List<MimeTypeStatsResult> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(final List<MimeTypeStatsResult> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    @JsonProperty(value = "resource_count", access = READ_ONLY)
    public Long getResourceCount() {
        return isEmpty(this.mimeTypes) ? 0 :
                this.mimeTypes.stream().mapToLong(x -> x.getResourceCount()).sum();
    }

    @JsonProperty(value = "byte_count", access = READ_ONLY)
    public Long getByteCount() {
        return isEmpty(this.mimeTypes) ? 0 :
                this.mimeTypes.stream().mapToLong(x -> x.getByteCount()).sum();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this).toString();
    }
}
