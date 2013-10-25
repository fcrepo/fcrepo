/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.utils.iterators;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterators.filter;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;

import static org.fcrepo.kernel.RdfLexicon.isManagedPredicate;
import java.util.Iterator;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * This wraps an {@link Iterator} of {@link Triple}s and produces only those
 * safe for persistence into the repo as properties. It does this by filtering
 * all triples with managed predicates, as defined in {@link RdfLexicon}.
 *
 * @author ajs6f
 * @date Oct 23, 2013
 */
public class UnmanagedRdfStream extends RdfStream {

    private static final Model model = createDefaultModel();

    public static final Predicate<Triple> isManagedTriple =
        new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {
                return isManagedPredicate.apply(model.asStatement(t)
                        .getPredicate());
            }

        };

    /**
     * Ordinary constructor.
     *
     * @param triples
     */
    public UnmanagedRdfStream(final Iterator<Triple> triples) {
        super(triples);
    }

    @Override
    protected Iterator<Triple> delegate() {
        return filter(triples, not(isManagedTriple));
    }

}