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
                left.getSubject().toString(prefixMapping, false).compareTo(
                        right.getSubject().toString(prefixMapping, false));

        if (s != 0) {
            return s;
        }

        final int p =
                left.getPredicate().toString(prefixMapping, false).compareTo(
                        right.getPredicate().toString(prefixMapping, false));

        if (p != 0) {
            return p;
        }

        return left.getObject().toString(false).compareTo(
                right.getObject().toString(false));

    }

}
