/*
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

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.RdfContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.EnumSet;
import java.util.stream.Stream;

import static java.util.EnumSet.of;
import static org.fcrepo.kernel.api.rdf.RdfContext.RDF_TYPE;
import static org.fcrepo.kernel.api.rdf.RdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.rdf.RdfContext.SKOLEM;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class HashRdfContext extends NodeRdfContext {


    private static final EnumSet<RdfContext> contexts = of(RDF_TYPE, PROPERTIES, SKOLEM);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public HashRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        this.stream = getNodeStream(resource)
                .flatMap(n -> nodeConverter.convert(n).getTriples(idTranslator, contexts));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Node> getNodeStream(final FedoraResource resource) throws RepositoryException {
        final Node node = resource.getNode();
        if (node.hasNode("#")) {
            return iteratorToStream(node.getNode("#").getNodes());
        } else {
            return Stream.empty();
        }
    }
}
