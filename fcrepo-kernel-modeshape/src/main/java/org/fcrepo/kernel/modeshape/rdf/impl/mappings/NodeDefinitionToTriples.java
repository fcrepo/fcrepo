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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDFS.range;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility for moving Node Definitions into RDFS triples
 * @author cbeer
 */
public class NodeDefinitionToTriples extends ItemDefinitionToTriples<NodeDefinition> {

    private static final Logger LOGGER = getLogger(NodeDefinitionToTriples.class);

    /**
     * Translate ItemDefinitions into triples. The definitions will hang off
     * the provided RDF Node
     * @param domain the domain
     */
    public NodeDefinitionToTriples(final Node domain) {
        super(domain);
    }

    @Override
    public Iterator<Triple> apply(final NodeDefinition input) {

        try {

            final Node propertyDefinitionNode = getResource(input).asNode();

            final NodeType[] requiredPrimaryTypes = input.getRequiredPrimaryTypes();

            if (requiredPrimaryTypes.length > 1) {
                // TODO we can express this as an OWL unionOf. But should we?
                LOGGER.trace(
                        "Skipping RDFS:range for {} with multiple primary types",
                        propertyDefinitionNode.getName());
            } else if (requiredPrimaryTypes.length == 1) {
                LOGGER.trace("Adding RDFS:range for {} with primary types {}",
                             input.getName(),
                             requiredPrimaryTypes[0].getName());
                return new RdfStream(create(propertyDefinitionNode, range
                        .asNode(), getResource(requiredPrimaryTypes[0])
                        .asNode())).concat(super.apply(input));
            } else {
                LOGGER.trace("Skipping RDFS:range for {} with no required primary types");
            }
            return super.apply(input);

        } catch (final RepositoryException e) {
            throw propagate(e);
        }

    }
}
