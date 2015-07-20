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

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.Property;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.domain;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static org.fcrepo.kernel.modeshape.rdf.impl.mappings.ItemDefinitionToTriples.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>ItemDefinitionToTriplesTest class.</p>
 *
 * @author cbeer
 */
public class ItemDefinitionToTriplesTest {

    @InjectMocks
    private ItemDefinitionToTriples<ItemDefinition> testMapper;

    @Mock
    private NamespacedItemDefinition mockItemDefinition;

    @Mock
    private NamespacedNodeType mockNodeType;

    @Mock
    private Node mockRDFNode = createURI("mock:domain");

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockItemDefinition.getName()).thenReturn("mock:ItemDefinition");
        when(mockItemDefinition.getLocalName()).thenReturn("ItemDefinition");
        when(mockItemDefinition.getNamespaceURI()).thenReturn("mock");
    }

    @Test
    public void testGoodDefinition() throws RepositoryException {
        final Set<Triple> results =
            copyOf(testMapper.apply(mockItemDefinition));
        LOGGER.debug("Created RDF: ");
        for (final Triple t : results) {
            LOGGER.debug("{}", t);
        }

        final Node subject =
            getResource((ItemDefinition) mockItemDefinition).asNode();
        assertTrue(results.contains(create(subject, type.asNode(), Property
                .asNode())));
        assertTrue(results.contains(create(subject, label.asNode(),
                createLiteral(mockItemDefinition.getName()))));
        assertTrue(results.contains(create(subject, domain.asNode(),
                mockRDFNode)));
    }

    @Test(expected = RuntimeException.class)
    public void testBadDefinition() throws RepositoryException {
        when(mockItemDefinition.getNamespaceURI()).thenThrow(
                new RepositoryException("Expected."));
        testMapper.apply(mockItemDefinition);
    }

    @Test
    public void testGetResourceFromNodeType() throws RepositoryException {
        when(mockNodeType.getNamespaceURI()).thenReturn("namespace#");
        when(mockNodeType.getLocalName()).thenReturn("localname");
        final Resource answer = getResource((NodeType) mockNodeType);
        assertEquals("namespace#", answer.getNameSpace());
        assertEquals("localname", answer.getLocalName());
    }

    private static interface NamespacedItemDefinition extends Namespaced,
            ItemDefinition {
        // for mocking purposes
    }

    private static interface NamespacedNodeType extends Namespaced, NodeType {
        // for mocking purposes
    }

    private static final Logger LOGGER =
        getLogger(ItemDefinitionToTriplesTest.class);

}
