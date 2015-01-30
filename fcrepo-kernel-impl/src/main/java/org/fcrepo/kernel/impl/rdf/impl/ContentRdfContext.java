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
package org.fcrepo.kernel.impl.rdf.impl;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.RdfLexicon.DESCRIBED_BY;

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @since 10/16/14
 */
public class ContentRdfContext extends NodeRdfContext {
    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public ContentRdfContext(final FedoraResource resource,
                             final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        // if there's an accessible jcr:content node, include information about
        // it
        if (resource instanceof NonRdfSourceDescription) {
            final FedoraResource contentNode = ((NonRdfSourceDescription) resource()).getDescribedResource();
            final Node subject = translator().reverse().convert(resource()).asNode();
            final Node contentSubject = translator().reverse().convert(contentNode).asNode();
            // add triples representing parent-to-content-child relationship
            concat(create(subject, DESCRIBES.asNode(), contentSubject));

        } else if (resource instanceof FedoraBinary) {
            final FedoraResource description = ((FedoraBinary) resource).getDescription();
            concat(create(translator().reverse().convert(resource).asNode(),
                    DESCRIBED_BY.asNode(),
                    translator().reverse().convert(description).asNode()));
        }
    }
}
