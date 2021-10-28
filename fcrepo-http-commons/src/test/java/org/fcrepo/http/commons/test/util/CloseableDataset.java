/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.test.util;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetImpl;

/**
 * Adds the standard {@link AutoCloseable} semantic to Jena's {@link org.apache.jena.query.Dataset} for
 * convenient use with Java 7's <code>try-with-resources</code> syntax.
 *
 * @author ajs6f
 */
public class CloseableDataset extends DatasetImpl implements AutoCloseable {

    /**
     * Default constructor.
     *
     * @param model Model to wrap
     */
    public CloseableDataset(final Model model) {
        super(model);
    }
}
