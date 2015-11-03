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
package org.fcrepo.kernel.modeshape.rdf;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;

import java.util.function.Predicate;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * {@link Predicate}s for determining when RDF is managed by the repository.
 *
 * @author ajs6f
 * @since Oct 23, 2013
 */
public final class ManagedRdf {

    private static final Model model = createDefaultModel();

    /**
     * No public constructor on utility class
     */
    private ManagedRdf() {
    }

    public static final Predicate<Triple> isManagedTriple =
        p -> isManagedPredicate.test(model.asStatement(p).getPredicate());

    public static final Predicate<Resource> isManagedMixin =
        p -> isManagedNamespace.test(p.getNameSpace());
}
