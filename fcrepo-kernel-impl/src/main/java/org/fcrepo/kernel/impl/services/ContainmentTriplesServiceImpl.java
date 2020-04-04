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
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;

import javax.inject.Inject;

import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ContainmentTriplesService;
import org.springframework.stereotype.Component;

/**
 * Containment Triples service.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class ContainmentTriplesServiceImpl implements ContainmentTriplesService {

    @Inject
    private ContainmentIndex containmentIndex;

    @Override
    public Stream<Triple> get(final Transaction tx, final FedoraResource resource) {
        final Node currentNode = createURI(resource.getFedoraId().getFullId());
        return containmentIndex.getContains(tx, resource).map(c ->
                new Triple(currentNode, CONTAINS.asNode(), createURI(c)));
    }
}
