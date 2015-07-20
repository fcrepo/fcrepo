/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api.utils.iterators;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.sparql.graph.GraphFactory.createDefaultGraph;

/**
 * A wrapping {@link Iterator} that calculates two differences between a
 * {@link Graph} A and a source Iterator B. The differences are (A - (A ∩ B)) and
 * (B - (A ∩ B)). The ordinary output of this iterator is (B - (A ∩ B)), and
 * after exhaustion, sets containing (A - (A ∩ B)) and (A ∩ B) are available.
 *
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class GraphDifferencingIterator extends AbstractIterator<Triple> {

    private Graph notCommon;

    private Graph common;

    private Iterator<Triple> source;

    /**
     * Diff a Model against a stream of triples
     *
     * @param replacement the replacement
     * @param original the original
     */
    public GraphDifferencingIterator(final Model replacement,
                                     final Iterator<Triple> original) {
        this(replacement.getGraph(), original);
    }

    /**
     * Diff a graph against a stream of triples
     *
     * @param replacement the replacement
     * @param original the original
     */
    public GraphDifferencingIterator(final Graph replacement,
                                     final Iterator<Triple> original) {
        super();
        this.notCommon = replacement;
        this.common = createDefaultGraph();
        this.source = original;

    }

    @Override
    protected Triple computeNext() {
        if (source.hasNext()) {
            Triple next = source.next();
            // we only want to return this element if it is not common
            // to the two inputs
            while (common.contains(next) || notCommon.contains(next)) {
                // it was common, so shift it to common
                if (notCommon.contains(next)) {
                    notCommon.remove(next.getSubject(), next.getPredicate(), next.getObject());
                    common.add(next);
                }
                // move onto the next candidate
                if (!source.hasNext()) {
                    return endOfData();
                }
                next = source.next();
            }
            // it was not common so return it
            return next;
        }
        return endOfData();
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out to be common to the two inputs.
     */
    public Iterator<Triple> common() {
        return source.hasNext() ? null : common.find(ANY, ANY, ANY);
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out not to be common to the two inputs.
     */
    public Iterator<Triple> notCommon() {
        return source.hasNext() ? null : notCommon.find(ANY, ANY, ANY);
    }

}
