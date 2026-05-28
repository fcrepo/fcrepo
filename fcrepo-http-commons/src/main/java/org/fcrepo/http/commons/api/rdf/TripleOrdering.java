/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.api.rdf;

import java.util.Comparator;

import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;

/**
 * Comparator to sort a list of Quads by subject, predicate, and object
 * to ensure a consistent order for human-readable output
 *
 * @author awoods
 */
public class TripleOrdering implements Comparator<Triple> {

    private final PrefixMapping prefixMapping;

    /**
     * When sorting predicates, take into account the given PrefixMapping
     * @param prefixMapping the prefix mapping
     */
    public TripleOrdering(final PrefixMapping prefixMapping) {
        super();

        this.prefixMapping = prefixMapping;
    }

    @Override
    public int compare(final Triple left, final Triple right) {

        final int s =
                left.getSubject().toString(prefixMapping).compareTo(
                        right.getSubject().toString(prefixMapping));

        if (s != 0) {
            return s;
        }

        final int p =
                left.getPredicate().toString(prefixMapping).compareTo(
                        right.getPredicate().toString(prefixMapping));

        if (p != 0) {
            return p;
        }

        return left.getObject().toString().compareTo(
                right.getObject().toString());

    }

}
