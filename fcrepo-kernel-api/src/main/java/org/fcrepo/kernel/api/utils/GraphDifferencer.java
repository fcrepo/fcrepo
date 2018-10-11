/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api.utils;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.sparql.graph.GraphFactory.createDefaultGraph;

/**
 * A wrapping {@link Stream} that calculates two differences between a
 * {@link Graph} A and a source Stream B. The differences are (A - (A ∩ B)) and
 * (B - (A ∩ B)). The ordinary output of this stream is (B - (A ∩ B)), and
 * after exhaustion, sets containing (A - (A ∩ B)) and (A ∩ B) are available.
 *
 * @author ajs6f
 * @author acoburn
 * @since Oct 24, 2013
 */
public class GraphDifferencer {

    private final Graph notCommon;

    private final Graph common;

    private final Stream.Builder<Triple> source = Stream.builder();

    /**
     * Diff a Model against a stream of triples
     *
     * @param replacement the replacement
     * @param original the original
     */
    public GraphDifferencer(final Model replacement,
                                     final Stream<Triple> original) {
        this(replacement.getGraph(), original);
    }

    /**
     * Diff a graph against a stream of triples
     *
     * @param replacement the replacement
     * @param original the original
     */
    public GraphDifferencer(final Graph replacement,
                                     final Stream<Triple> original) {
        notCommon = replacement;
        common = createDefaultGraph();
        original.forEach(x -> {
            synchronized (this) {
                if (notCommon.contains(x)) {
                    notCommon.remove(x.getSubject(), x.getPredicate(), x.getObject());
                    common.add(x);
                } else if (!common.contains(x)) {
                    source.accept(x);
                }
            }
        });
    }

    /**
     * This method returns the difference between the two input sources.
     *
     * @return The differences between the two inputs.
     */
    public Stream<Triple> difference() {
        return source.build();
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out to be common to the two inputs.
     */
    public Stream<Triple> common() {
        return stream(spliteratorUnknownSize(common.find(ANY, ANY, ANY), IMMUTABLE), false);
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out not to be common to the two inputs.
     */
    public Stream<Triple> notCommon() {
        return stream(spliteratorUnknownSize(notCommon.find(ANY, ANY, ANY), IMMUTABLE), false);
    }
}
