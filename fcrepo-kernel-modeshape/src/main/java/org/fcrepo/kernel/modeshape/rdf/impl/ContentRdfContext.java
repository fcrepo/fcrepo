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

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;

import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/16/14
 */
public class ContentRdfContext extends NodeRdfContext {
    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     */
    public ContentRdfContext(final FedoraResource resource,
                             final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        // if there's an accessible jcr:content node, include information about it
        if (resource instanceof NonRdfSourceDescription) {
            final FedoraResource contentNode = resource().getDescribedResource();
            final Node subject = uriFor(resource());
            final Node contentSubject = uriFor(contentNode);
            // add triples representing parent-to-content-child relationship
            concat(of(create(subject, DESCRIBES.asNode(), contentSubject)));

        } else if (resource instanceof FedoraBinary) {
            final FedoraResource description = resource.getDescription();
            concat(of(create(uriFor(resource), DESCRIBED_BY.asNode(), uriFor(description))));
        }
    }
}
