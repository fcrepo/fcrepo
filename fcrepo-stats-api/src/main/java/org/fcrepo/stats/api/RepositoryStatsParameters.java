/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

import java.util.List;

/**
 * A parameter object that allows you to narrow your statistics query
 * @author dbernstein
 */
public class RepositoryStatsParameters {

    private List<String> mimeTypes;
    private List<String> rdfTypes;

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(final List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public RepositoryStatsParameters() {
    }

    public void setRdfTypes(final List<String> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }

    public List<String> getRdfTypes() {
        return rdfTypes;
    }
}
