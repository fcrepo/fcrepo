/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An RDF Type Stats data object.
 * @author dbernstein
 */
public class RepositoryStatsByRdfTypeResults {

    @JsonProperty("rdf_types")
    private List<RdfTypeStatsResult> rdfTypes = new ArrayList<>();

    public List<RdfTypeStatsResult> getRdfTypes() {
        return rdfTypes;
    }

    public void setRdfTypes(final List<RdfTypeStatsResult> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }
}
