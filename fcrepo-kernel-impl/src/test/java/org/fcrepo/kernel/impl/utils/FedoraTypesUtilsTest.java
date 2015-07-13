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
package org.fcrepo.kernel.impl.utils;

import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_SKOLEM;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isBlankNode;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isReferenceProperty;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isBinaryContentProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalReferenceProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isNonRdfSourceDescription;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isContainer;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isExternalNode;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.services.functions.JcrPropertyFunctions;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrConstants;

/**
 * <p>FedoraTypesUtilsTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraTypesUtilsTest {

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private ValueFactory mockVF;

    @Mock
    private InputStream mockInput;

    @Mock
    private JcrValueFactory mockJVF;

    @Mock
    private Workspace mockWS;

    @Mock
    private NodeTypeManager mockNTM;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private PropertyDefinition mockPropertyDefinition;

    @Mock
    private Version mockVersion;

    @Mock
    private VersionManager mockVersionManager;

    @Mock
    private VersionHistory mockVersionHistory;

    @Mock
    private Repository mockRepository;

    @Mock
    private QueryManager mockQueryManager;

    @Mock
    private Query mockQuery;

    @Mock
    private QueryResult mockResults;

    @Mock
    private RowIterator mockIterator;

    @Mock
    private Row mockRow;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockValue2;

    @Mock
    private Property mockProperty;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockContainer;

    @Mock
    private JcrRepository mockJcrRepository;

    @Mock
    private RepositoryConfiguration mockConfig;

    @Test
    public void testIsBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertTrue(isBinaryContentProperty.test(mockProperty));
    }

    @Test
    public void testIsInternalReferenceProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.REFERENCE);
        when(mockProperty.getName()).thenReturn(getReferencePropertyName("foo"));
        assertTrue(isInternalReferenceProperty.test(mockProperty));
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testIsInternalReferencePropertyException() throws RepositoryException {
        when(mockProperty.getType()).thenThrow(new RepositoryException());
        assertTrue(isInternalReferenceProperty.test(mockProperty));
    }

    @Test
    public void testIsInternalReferencePropertyWeak() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.WEAKREFERENCE);
        when(mockProperty.getName()).thenReturn(getReferencePropertyName("foo"));
        assertTrue(isInternalReferenceProperty.test(mockProperty));
    }

    @Test
    public void testIsReferenceProperty() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[] { mockPropertyDefinition });
        when(mockPropertyDefinition.getName()).thenReturn("some:reference_property");
        when(mockPropertyDefinition.getRequiredType()).thenReturn(PropertyType.REFERENCE);
        assertTrue(isReferenceProperty(mockNode, "some:reference_property"));
    }

    @Test
    public void testIsWeakReferenceProperty() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[] { mockPropertyDefinition });
        when(mockPropertyDefinition.getName()).thenReturn("some:reference_property");
        when(mockPropertyDefinition.getRequiredType()).thenReturn(PropertyType.WEAKREFERENCE);
        assertTrue(isReferenceProperty(mockNode, "some:reference_property"));
    }

    @Test
    public void testIsReferencePropertyForOtherPropertyTypes() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[] { mockPropertyDefinition });
        when(mockPropertyDefinition.getName()).thenReturn("some:reference_property");
        when(mockPropertyDefinition.getRequiredType()).thenReturn(PropertyType.BINARY);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { });
        assertFalse(isReferenceProperty(mockNode, "some:reference_property"));
    }

    @Test
    public void testIsReferencePropertyForMissingTypes() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[] {  });
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { });
        assertFalse(isReferenceProperty(mockNode, "some:reference_property"));
    }

    @Test
    public void testIsInternalProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertTrue(isInternalProperty.test(mockProperty));
    }

    @Test
    public void testIsNotBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.STRING);
        assertFalse(isBinaryContentProperty.test(mockProperty));
    }

    @Test
    public void testContentButNotBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.STRING);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertFalse(isBinaryContentProperty.test(mockProperty));
    }

    @Test
    public void testIsBlanknode() throws RepositoryException {
        when(mockNode.isNodeType(FEDORA_SKOLEM)).thenReturn(true);
        assertTrue("Expected to be a blank node", isBlankNode.test(mockNode));

        when(mockNode.isNodeType(FEDORA_SKOLEM)).thenReturn(false);
        assertFalse("Expected to not be a blank node", isBlankNode.test(mockNode));
    }

    @Test
    public void testIsInternalNode() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.isNodeType("mode:system")).thenReturn(true);
        assertTrue("mode:system nodes should be treated as internal nodes!",
                isInternalNode.test(mockNode));

        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.isNodeType("mode:system")).thenReturn(false);
        assertFalse("Nodes that are not mode:system types should not be "
                + "treated as internal nodes!", isInternalNode.test(mockNode));

        when(mockNode.isNodeType("mode:system")).thenThrow(new RepositoryException());
        try {
            isInternalNode.test(mockNode);
            fail("Unexpected completion of FedoraTypesUtils.isInternalNode" +
                 " after RepositoryException!");
        } catch (final RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testPredicateExceptionHandling() throws RepositoryException {
        when(mockNode.getMixinNodeTypes()).thenThrow(new RepositoryException());
        when(mockNode.isNodeType(anyString())).thenThrow(new RepositoryException());

        try {
            isContainer.test(mockNode);
            fail("Unexpected FedoraTypesUtils.isContainer" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {
            // expected
        }
        try {
            isNonRdfSourceDescription.test(mockNode);
            fail("Unexpected FedoraTypesUtils.isNonRdfSourceDescription" +
                 " completion after RepositoryException!");
        } catch (final RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testProperty2values() throws RepositoryException {
        // single-valued
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getValue()).thenReturn(mockValue);
        assertEquals("Found wrong Value!", JcrPropertyFunctions.property2values
                .apply(mockProperty).next(), mockValue);
        // multi-valued
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getValues()).thenReturn(
                new Value[] {mockValue, mockValue2});
        final Iterator<Value> testIterator = JcrPropertyFunctions.property2values.apply(mockProperty);
        assertEquals("Found wrong Value!", testIterator.next(), mockValue);
        assertEquals("Found wrong Value!", testIterator.next(), mockValue2);

    }

    @Test
    public void testGetClosestExistingAncestorRoot() throws RepositoryException {
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockSession.nodeExists(anyString())).thenReturn(false);

        final Node closestExistingAncestor = getClosestExistingAncestor(mockSession, "/some/path");
        assertEquals(mockRootNode, closestExistingAncestor);
    }

    @Test
    public void testGetClosestExistingAncestorContainer() throws RepositoryException {
        when(mockSession.getNode("/")).thenReturn(mockRootNode);
        when(mockSession.nodeExists("/some")).thenReturn(true);
        when(mockSession.getNode("/some")).thenReturn(mockContainer);

        final Node closestExistingAncestor = getClosestExistingAncestor(mockSession, "/some/path");
        assertEquals(mockContainer, closestExistingAncestor);
    }

    @Test
    public void testGetClosestExistingAncestorNode() throws RepositoryException {
        when(mockSession.getNode("/")).thenReturn(mockRootNode);
        when(mockSession.nodeExists("/some")).thenReturn(true);
        when(mockSession.getNode("/some")).thenReturn(mockContainer);
        when(mockSession.nodeExists("/some/path")).thenReturn(true);
        when(mockSession.getNode("/some/path")).thenReturn(mockNode);

        final Node closestExistingAncestor = getClosestExistingAncestor(mockSession, "/some/path");
        assertEquals(mockNode, closestExistingAncestor);
    }

    @Test
    public void testIsExternalNode1() throws RepositoryException {
        when(mockNode.getIdentifier()).thenReturn(UUID.randomUUID().toString());
        assertFalse(isExternalNode.test(mockNode));
    }

    @Test
    public void testIsExternalNode2() throws RepositoryException {
        // sha1 of "BinaryStore" is 952357dbe6acf9e88a6d0164807a79a40993003f
        // so sourceKey is 952357d
        when(mockNode.getIdentifier()).thenReturn("952357ddefault/some/path");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockJcrRepository);
        when(mockJcrRepository.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getStoreName()).thenReturn("BinaryStore");
        assertFalse(isExternalNode.test(mockNode));
    }

    @Test
    public void testIsExternalNode3() throws RepositoryException {
        // sha1 of "BinaryStore" is 952357dbe6acf9e88a6d0164807a79a40993003f
        // so sourceKey is 952357d which doesn't match 07f66ed
        when(mockNode.getIdentifier()).thenReturn("07f66eddefault/some/path");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockJcrRepository);
        when(mockJcrRepository.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getStoreName()).thenReturn("BinaryStore");
        assertTrue(isExternalNode.test(mockNode));
    }
}
