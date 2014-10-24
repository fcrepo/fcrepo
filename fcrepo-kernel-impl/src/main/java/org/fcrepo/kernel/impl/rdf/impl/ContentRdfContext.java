/**
 * Copyright 2014 DuraSpace, Inc.
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
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
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
        if (resource() instanceof Datastream) {
            final FedoraResource contentNode = ((Datastream) resource()).getBinary();
            final Node contentSubject = translator().reverse().convert(contentNode).asNode();
            final Node subject = translator().reverse().convert(resource()).asNode();
            // add triples representing parent-to-content-child relationship
            concat(new Triple[] {
                    create(subject, HAS_CONTENT.asNode(), contentSubject),
                    create(contentSubject, IS_CONTENT_OF.asNode(), subject)});

        }
    }
}
