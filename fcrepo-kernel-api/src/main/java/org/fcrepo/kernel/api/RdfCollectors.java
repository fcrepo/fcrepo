/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

import java.util.stream.Collector;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

/**
 * A class of Collectors for use with RdfStreams
 * @author acoburn
 * @since Dec 4, 2015
 */
public class RdfCollectors {

    /**
     * @return a Collector for use with aggregating an RdfStream into a Model.
     */
    public static Collector<Triple, ?, Model> toModel() {
        return Collector.of(ModelFactory::createDefaultModel,
            (m, t) -> m.add(m.asStatement(t)),
            (left, right) -> {
                    left.add(right);
                    return left;
                },
            Collector.Characteristics.UNORDERED);
    }

    private RdfCollectors() {
        // prevent instantiation
    }
}

