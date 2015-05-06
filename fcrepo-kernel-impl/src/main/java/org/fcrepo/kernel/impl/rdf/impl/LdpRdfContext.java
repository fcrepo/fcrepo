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
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.RdfLexicon.RDF_SOURCE;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.models.NonRdfSource;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

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


        if (resource instanceof NonRdfSource) {
            concat(nonRdfSourceContext());
        } else {
            concat(typeContext());
        }

        if (resource instanceof Container) {
            concat(containerContext());

            if (!resource.hasType(FEDORA_CONTAINER)) {
                concat(defaultContainerContext());
            }
        }

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
