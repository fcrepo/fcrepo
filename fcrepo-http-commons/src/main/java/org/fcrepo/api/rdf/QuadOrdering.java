package org.fcrepo.api.rdf;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.Quad;

import java.util.Comparator;

public class QuadOrdering implements Comparator<Quad> {

    private final PrefixMapping prefixMapping;

    public QuadOrdering(final PrefixMapping prefixMapping) {
        super();

        this.prefixMapping = prefixMapping;
    }

    @Override
    public int compare(com.hp.hpl.jena.sparql.core.Quad left, com.hp.hpl.jena.sparql.core.Quad right) {

        final int s = left.getSubject().toString(prefixMapping, false).compareTo(right.getSubject().toString(prefixMapping, false));

        if (s != 0) {
            return s;
        }

        final int p = left.getPredicate().toString(prefixMapping, false).compareTo(right.getPredicate().toString(prefixMapping, false));

        if (p != 0) {
            return p;
        }

        return left.getObject().toString(false).compareTo(right.getObject().toString(false));

    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
