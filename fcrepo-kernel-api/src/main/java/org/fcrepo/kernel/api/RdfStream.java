/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * A context-bearing RDF Stream interface
 *
 * @author acoburn
 * @since Dec 4, 2015
 */
public interface RdfStream extends Stream<Triple> {

    /**
     * @return the topic node for this stream
     */
    Node topic();
}
