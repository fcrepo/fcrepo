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

package org.fcrepo.kernel.identifiers;

import com.google.common.base.Function;
import com.hp.hpl.jena.graph.Triple;

/**
 * Deskolemization is abstractly a function from RDF nodes to RDF nodes, but here we implement it, purely for
 * convenience of operation, as a function from triples to triples. An implementing object should be used to
 * translate only one document's scope of RDF.
 *
 * @author ajs6f
 */
public interface Deskolemizer extends Function<Triple, Triple> {
}
