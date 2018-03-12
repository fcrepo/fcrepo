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
package org.fcrepo.kernel.modeshape;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.Calendar.JULY;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.testutilities.TestNodeIterator.nodeIterator;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.time.Instant;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.JcrRdfTools;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * <p>FedoraResourceImplTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraResourceImplTest {

    private FedoraResource testObj;

    @Mock
    private Node mockNode, mockRoot, mockChild, mockContainer;

    @Mock
    private NodeType mockPrimaryNodeType, mockMixinNodeType, mockPrimarySuperNodeType, mockMixinSuperNodeType;

    @Mock
    private Session mockSession;

    @Mock
    private Property mockProp, mockContainerProperty;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private JcrRdfTools mockJcrRdfTools;

    @Mock
    private IdentifierConverter<Resource, FedoraResource> mockSubjects;

    @Before
    public void setUp() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/some/path");
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        testObj = new FedoraResourceImpl(mockNode);
        assertEquals(mockNode, getJcrNode(testObj));

        mockSubjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testGetPath() throws RepositoryException {
        testObj.getPath();
        verify(mockNode).getPath();
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testGetPathException() throws RepositoryException {
        doThrow(RepositoryException.class).when(mockNode).getPath();
        testObj.getPath();
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Calendar someDate = Calendar.getInstance();
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        assertEquals(someDate.getTimeInMillis(), testObj.getCreatedDate().toEpochMilli());
    }

    @Test
    public void testGetTypes() throws RepositoryException {
        final String mockNodeTypePrefix = "jcr";

        final String mockPrimaryNodeTypeName = "somePrimaryType";
        final String mockMixinNodeTypeName = "someMixinType";
        final String mockPrimarySuperNodeTypeName = "somePrimarySuperType";
        final String mockMixinSuperNodeTypeName = "someMixinSuperType";

        final Workspace mockWorkspace = mock(Workspace.class);
        final NamespaceRegistry mockNamespaceRegistry = mock(NamespaceRegistry.class);

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(mockPrimaryNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockPrimaryNodeTypeName);

        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[]{mockMixinNodeType});
        when(mockMixinNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockMixinNodeTypeName);

        when(mockPrimaryNodeType.getSupertypes()).thenReturn(
                new NodeType[]{mockPrimarySuperNodeType});
        when(mockPrimarySuperNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockPrimarySuperNodeTypeName);

        when(mockMixinNodeType.getSupertypes()).thenReturn(
                new NodeType[] {mockMixinSuperNodeType, mockMixinSuperNodeType});
        when(mockMixinSuperNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockMixinSuperNodeTypeName);

        final List<URI> types = testObj.getTypes();
        assertFalse(types.contains(URI.create(REPOSITORY_NAMESPACE + mockPrimaryNodeTypeName)));
        assertFalse(types.contains(URI.create(REPOSITORY_NAMESPACE + mockMixinNodeTypeName)));
        assertFalse(types.contains(URI.create(REPOSITORY_NAMESPACE + mockPrimarySuperNodeTypeName)));
        assertFalse(types.contains(URI.create(REPOSITORY_NAMESPACE + mockMixinSuperNodeTypeName)));
        assertEquals(0, types.size());
    }

    @Test
    public void testGetLastModifiedDateDefault() throws RepositoryException {
        // test missing JCR_LASTMODIFIED/FEDORA_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockNode.hasProperty(FEDORA_LASTMODIFIED)).thenReturn(false);
            when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(false);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Instant actual = testObj.getLastModifiedDate();
        assertEquals(someDate.getTimeInMillis(), actual.toEpochMilli());
        // this is a read operation, it must not persist the session
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetLastModifiedDate() {
        // test existing FEDORA_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        try {
            when(mockNode.hasProperty(FEDORA_LASTMODIFIED)).thenReturn(true);
            when(mockNode.getProperty(FEDORA_LASTMODIFIED)).thenReturn(mockMod);
            when(mockMod.getDate()).thenReturn(modDate);
        } catch (final RepositoryException e) {
            System.err.println("What are we doing in the second test?");
            e.printStackTrace();
        }
        final Instant actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.toEpochMilli());
    }

    @Test
    public void testTouch() throws RepositoryException {
        // test existing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        when(mockNode.getSession()).thenReturn(mockSession);
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockMod);
        when(mockMod.getDate()).thenReturn(modDate);
        final Instant actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.toEpochMilli());
    }

    @Test
    public void testIsNew() {
        when(mockNode.isNew()).thenReturn(true);
        assertTrue("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test
    public void testIsNotNew() {
        when(mockNode.isNew()).thenReturn(false);
        assertFalse("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test(expected = MalformedRdfException.class)
    public void testReplacePropertiesDataset() throws RepositoryException {

        final DefaultIdentifierTranslator defaultGraphSubjects = new DefaultIdentifierTranslator(mockSession);

        when(mockNode.getPath()).thenReturn("/xyz");
        when(mockSession.getNode("/xyz")).thenReturn(mockNode);

        final Model propertiesModel = createDefaultModel();
        propertiesModel.add(propertiesModel.createResource("a"),
            propertiesModel.createProperty("b"),
            "c");

        try (final RdfStream propertiesStream = fromModel(createURI("info:fedora/xyz"), propertiesModel)) {

            final Model replacementModel = createDefaultModel();

            replacementModel.add(replacementModel.createResource("a"), replacementModel.createProperty("b"), "n");

            testObj.replaceProperties(defaultGraphSubjects, replacementModel, propertiesStream);
        }
    }


    @Test(expected = IllegalArgumentException.class)
    public void testUpdateInvalidObjectUrlInSparql() throws RepositoryException {
        testUpdateInvalidSPARQL(
                "INSERT DATA {<> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://pcdm.org/models##file> .}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateInvalidPredicateUrlInSparql() throws RepositoryException {
        testUpdateInvalidSPARQL("INSERT DATA {<> <http://www.w3.org/1999/02/> <http://pcdm.org/models#file> .}");
    }

    private void testUpdateInvalidSPARQL(final String sparqlUpdateStatement) throws RepositoryException {

        final DefaultIdentifierTranslator defaultGraphSubjects = new DefaultIdentifierTranslator(mockSession);

        when(mockNode.getPath()).thenReturn("/xyz");
        when(mockSession.getNode("/xyz")).thenReturn(mockNode);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        final Model propertiesModel = createDefaultModel();
        try (final RdfStream propertiesStream = fromModel(createURI("info:fedora/xyz"), propertiesModel)) {

            testObj.updateProperties(defaultGraphSubjects, sparqlUpdateStatement, propertiesStream);

        }
    }

    @Test
    public void shouldGetEtagForAnObject() throws RepositoryException {
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        modDate.set(2013, JULY, 30, 0, 0, 0);
        when(mockNode.getPath()).thenReturn("some-path");
        when(mockNode.hasProperty(FEDORA_LASTMODIFIED)).thenReturn(true);
        when(mockNode.getProperty(FEDORA_LASTMODIFIED)).thenReturn(mockMod);
        when(mockMod.getDate()).thenReturn(modDate);

        assertEquals(sha1Hex("some-path"
                + testObj.getLastModifiedDate().toEpochMilli()), testObj
                .getEtagValue());
    }

    @Test
    public void testGetContainer() throws RepositoryException {
        when(mockNode.getParent()).thenReturn(mockContainer);
        when(mockNode.getDepth()).thenReturn(1);
        final FedoraResource actual = testObj.getContainer();
        assertEquals(new FedoraResourceImpl(mockContainer), actual);
    }

    @Test
    public void testGetContainerForNestedResource() throws RepositoryException {
        when(mockNode.getParent()).thenReturn(mockChild);
        when(mockNode.getDepth()).thenReturn(3);
        when(mockChild.getParent()).thenReturn(mockContainer);
        when(mockChild.getDepth()).thenReturn(2);
        when(mockChild.isNodeType(FEDORA_PAIRTREE)).thenReturn(true);
        when(mockContainer.getDepth()).thenReturn(1);
        final FedoraResource actual = testObj.getContainer();
        assertEquals(new FedoraResourceImpl(mockContainer), actual);
    }

    @Test
    public void testGetChild() throws RepositoryException {
        when(mockNode.getNode("xyz")).thenReturn(mockChild);
        final FedoraResource actual = testObj.getChild("xyz");
        assertEquals(new FedoraResourceImpl(mockChild), actual);
    }

    @Test
    public void testGetChildrenWithEmptyChildren() throws RepositoryException {
        when(mockNode.getNodes()).thenReturn(nodeIterator());
        final Stream<FedoraResource> children = testObj.getChildren();

        assertFalse("Expected an empty stream", children.findFirst().isPresent());
    }

    @Test
    public void testGetChildrenWithChildren() throws RepositoryException {
        when(mockNode.getNodes()).thenReturn(nodeIterator(mockChild));
        when(mockChild.getName()).thenReturn("x");
        final Optional<FedoraResource> child = testObj.getChildren().findFirst();

        assertTrue("Expected a stream with values", child.isPresent());
        assertEquals("Expected to find the child", mockChild, getJcrNode(child.get()));
    }

    @Test
    public void testGetChildrenExcludesModeSystem() throws RepositoryException {
        when(mockNode.getNodes()).thenReturn(nodeIterator(mockChild));
        when(mockChild.isNodeType("mode:system")).thenReturn(true);
        when(mockChild.getName()).thenReturn("x");
        final Stream<FedoraResource> children = testObj.getChildren();
        assertFalse("Expected an empty stream", children.findFirst().isPresent());
    }

    @Test
    public void testGetChildrenExcludesTombstones() throws RepositoryException {
        when(mockNode.getNodes()).thenReturn(nodeIterator(mockChild));
        when(mockChild.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        when(mockChild.getName()).thenReturn("x");
        final Stream<FedoraResource> children = testObj.getChildren();
        assertFalse("Expected an empty stream", children.findFirst().isPresent());
    }

    @Test
    public void testGetChildrenExcludesJcrContent() throws RepositoryException {
        when(mockNode.getNodes()).thenReturn(nodeIterator(mockChild));
        when(mockChild.getName()).thenReturn(JCR_CONTENT);
        final Stream<FedoraResource> children = testObj.getChildren();
        assertFalse("Expected an empty stream", children.findFirst().isPresent());
    }

    @Test
    public void testHasProperty() throws RepositoryException {
        when(mockNode.hasProperty("xyz")).thenReturn(true);
        final boolean actual = testObj.hasProperty("xyz");
        assertTrue("Expected same value as Node#hasProperty", actual);
    }

    @Test
    public void testGetProperty() throws RepositoryException {
        when(mockNode.getProperty("xyz")).thenReturn(mockProp);
        final Property actual = getJcrNode(testObj).getProperty("xyz");
        assertEquals(mockProp, actual);
    }

    @Test
    public void testEquals() {
        assertEquals(new FedoraResourceImpl(mockNode), new FedoraResourceImpl(mockNode));
        assertNotEquals(new FedoraResourceImpl(mockNode), new FedoraResourceImpl(mockRoot));
    }

    @Test
    public void testDelete() throws RepositoryException {
        when(mockNode.getReferences()).thenReturn(new TestPropertyIterator());
        when(mockNode.getWeakReferences()).thenReturn(new TestPropertyIterator());
        testObj.delete();
        verify(mockNode).remove();
    }

    @Test
    public void testDeleteLeavesATombstone() throws RepositoryException {
        when(mockNode.getReferences()).thenReturn(new TestPropertyIterator());
        when(mockNode.getWeakReferences()).thenReturn(new TestPropertyIterator());
        when(mockNode.getName()).thenReturn("a");
        when(mockNode.getParent()).thenReturn(mockContainer);
        when(mockNode.getDepth()).thenReturn(2);
        when(mockContainer.getNode("a")).thenThrow(new PathNotFoundException());
        when(mockContainer.getPath()).thenReturn("b");
        when(mockContainer.getSession()).thenReturn(mockSession);
        when(mockSession.nodeExists(anyString())).thenReturn(false);
        when(mockSession.getNode("b")).thenReturn(mockContainer);
        testObj.delete();
        verify(mockNode).remove();
        verify(mockContainer).addNode("a", FEDORA_TOMBSTONE);
    }
}
