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

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.fcrepo.kernel.api.utils.FedoraResourceIdConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;

/**
 * Retrieve the managed properties as triples
 *
 * @author dbernstein
 * @since 2020-01-07
 */
@Component
public class ManagedPropertiesServiceImpl implements ManagedPropertiesService {

    @Override
    public Stream<Triple> get(final FedoraResource resource) {
        final List<Triple> triples = new ArrayList<>();
        final var subject = createURI(FedoraResourceIdConverter.resolveFedoraId(resource));
        triples.add(Triple.create(subject, CREATED_DATE.asNode(),
                createLiteral(resource.getCreatedDate().toString())));
        triples.add(Triple.create(subject, LAST_MODIFIED_DATE.asNode(),
                createLiteral(resource.getLastModifiedDate().toString())));

        resource.getTypes().forEach(triple -> {
            triples.add(Triple.create(subject, type.asNode(), createURI(triple.toString())));
        });

        return new DefaultRdfStream(subject, triples.stream());
    }
}
