/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.time.Instant;
import java.util.List;

/**
 * A parameter object that allows you to narrow your statistics query
 * @author dbernstein
 */
public class StatsParameters {

    private List<String> mimeTypes;

    private List<String> rdfTypes;

    private Instant createdAfter;

    private Instant updatedBefore;

    /**
     * Constructor
     * @param mimeTypes Include only specified mimetypes types in stats count (null ok)
     * @param rdfTypes Include only specified RDF types in stats count (null ok)
     * @param createdAfter Include only resources created after this instant in stats count (null ok)
     * @param updatedBefore Include only resources updated after this instant in stats count (null ok)
     */
    public StatsParameters(final List<String> mimeTypes, final List<String> rdfTypes, final Instant createdAfter,
                           final Instant updatedBefore) {
        this.mimeTypes = mimeTypes;
        this.rdfTypes = rdfTypes;
        this.createdAfter = createdAfter;
        this.updatedBefore = updatedBefore;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public List<String> getRdfTypes() {
        return rdfTypes;
    }

    public Instant getCreatedAfter() {
        return createdAfter;
    }

    public Instant getUpdatedBefore() {
        return updatedBefore;
    }
}
