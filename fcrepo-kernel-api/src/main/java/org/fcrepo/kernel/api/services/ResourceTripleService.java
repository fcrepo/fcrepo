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
package org.fcrepo.kernel.api.services;

import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;

/**
 * Service to call other services to return a desired set of triples.
 * @author whikloj
 * @since 6.0.0
 */
public interface ResourceTripleService {

    /**
     * Return the triples for the resource based on the Prefer: header preferences
     * @param tx The transaction or null if none.
     * @param resource the resource to get triples for.
     * @param preferences the preferences asked for.
     * @param limit limit on the number of children to display.
     * @return a stream of triples.
     */
    Stream<Triple> getResourceTriples(final Transaction tx, final FedoraResource resource,
                                      final LdpTriplePreferences preferences, final int limit);
}
