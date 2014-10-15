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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.impl.testutilities.TestNodeIterator;
import org.fcrepo.kernel.impl.testutilities.TestPropertyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.jcr.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 */
public class LdpContainerRdfContextTest {
    private static final Logger LOGGER = getLogger(LdpContainerRdfContextTest.class);

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraResource mockContainer;

    @Mock
    private Node mockContainerNode;

    @Mock
    private Node mockChild;

    @Mock
    private Node mockNode;

    @Mock
    private PropertyIterator mockReferences;

    @Mock
    private Property mockProperty;

    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    private LdpContainerRdfContext testObj;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockResource.getPath()).thenReturn("/a");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockContainer.getNode()).thenReturn(mockContainerNode);
        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testLdpResource() throws RepositoryException {
        when(mockReferences.hasNext()).thenReturn(false);

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(mockReferences);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to be empty", model.isEmpty());
    }

    @Test
    public void testLdpResourceWithEmptyContainer() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to be empty", model.isEmpty());

    }

    @Test
    public void testLdpResourceWithContainer() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.hasNodes()).thenReturn(true);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to have one triple", model.size() == 1);
        assertTrue(model.contains(subjects.reverse().convert(mockResource),
                LDP_MEMBER,
                nodeToResource(subjects).convert(mockChild)));
    }


    @Test
    public void testLdpResourceWithContainerAssertingRelation() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.hasNodes()).thenReturn(true);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to have one triple", model.size() == 1);
        assertTrue(model.contains(
                subjects.reverse().convert(mockResource),
                ResourceFactory.createProperty("some:property"),
                nodeToResource(subjects).convert(mockChild)));
    }


}