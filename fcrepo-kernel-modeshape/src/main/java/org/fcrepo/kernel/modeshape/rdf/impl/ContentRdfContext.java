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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;

import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

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
            final FedoraResource contentNode = ((NonRdfSourceDescription) resource()).getDescribedResource();
            final Node subject = uriFor(resource());
            final Node contentSubject = uriFor(contentNode);
            // add triples representing parent-to-content-child relationship
            concat(create(subject, DESCRIBES.asNode(), contentSubject));

        } else if (resource instanceof FedoraBinary) {
            final FedoraResource description = ((FedoraBinary) resource).getDescription();
            concat(create(uriFor(resource), DESCRIBED_BY.asNode(), uriFor(description)));
        }
    }
}
