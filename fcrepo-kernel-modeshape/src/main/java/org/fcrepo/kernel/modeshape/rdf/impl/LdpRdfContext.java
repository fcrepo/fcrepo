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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;

import java.util.stream.Stream;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class LdpRdfContext extends NodeRdfContext {


    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public LdpRdfContext(final FedoraResource resource,
                         final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        final Stream.Builder<Triple> builder = Stream.builder();

        if (resource instanceof NonRdfSourceDescription) {
            builder.accept(nonRdfSourceContext());
        } else {
            builder.accept(typeContext());
        }

        if (resource instanceof Container) {
            builder.accept(containerContext());

            if (!resource.hasType(FEDORA_CONTAINER)) {
                builder.accept(defaultContainerContext());
            }
        }
        concat(builder.build());
    }

    private Triple typeContext() {
        return create(subject(), type.asNode(), RDF_SOURCE.asNode());
    }

    private Triple containerContext() {
        return create(subject(), type.asNode(), CONTAINER.asNode());
    }

    private Triple defaultContainerContext() {
        return create(subject(), type.asNode(), BASIC_CONTAINER.asNode());
    }

    private Triple nonRdfSourceContext() {
        return create(subject(), type.asNode(), NON_RDF_SOURCE.asNode());
    }
}
