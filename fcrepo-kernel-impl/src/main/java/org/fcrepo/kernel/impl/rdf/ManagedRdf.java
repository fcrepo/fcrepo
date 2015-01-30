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
package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.RdfLexicon.isManagedPredicate;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * {@link Predicate}s for determining when RDF is managed by the repository.
 *
 * @author ajs6f
 * @since Oct 23, 2013
 */
public class ManagedRdf {

    private static final Model model = createDefaultModel();

    /**
     * No public constructor on utility class
     */
    private ManagedRdf() {
    }

    public static final Predicate<Triple> isManagedTriple =
        new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {
                return isManagedPredicate.apply(model.asStatement(t)
                        .getPredicate());
            }

        };

    public static final Predicate<Resource> isManagedMixin =
        new Predicate<Resource>() {

            @Override
            public boolean apply(final Resource m) {
                return isManagedNamespace.apply(m.getNameSpace());
            }

        };
}
