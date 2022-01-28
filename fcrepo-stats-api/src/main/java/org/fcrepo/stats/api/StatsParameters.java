/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.time.Instant;
import java.util.List;

/**
 * @author dbernstein
 */
public class StatsParameters {
    public StatsParameters(final List<String> mimeTypes, final List<String> rdfTypes, final Instant createdAfter,
                           final Instant updatedBefore) {
    }
}
