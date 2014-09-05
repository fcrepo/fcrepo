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
package org.fcrepo.kernel.impl.utils;

import static java.util.Calendar.MILLISECOND;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getPredicateForProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.convertDateToXSDString;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getDefinitionForPropertyName;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isBinaryContentProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isReferenceProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraObject;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isMultipleValuedProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.nodeHasType;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.propertyContains;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.value2string;
import static org.fcrepo.kernel.impl.utils.NodePropertiesTools.REFERENCE_PROPERTY_SUFFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Namespaced;

import com.google.common.base.Predicate;

/**
 * <p>FedoraTypesUtilsTest class.</p>
 *
 * @author awoods
 */
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


    // unfortunately, we need to be able to cast to two interfaces to perform
    // some tests this testing interface allows mocks to do that
    interface PropertyMock extends Property, Namespaced {
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testIsMultipleValuedProperty() throws RepositoryException {
        final Property mockYes = mock(Property.class);
        when(mockYes.isMultiple()).thenReturn(true);
        final Property mockNo = mock(Property.class);
        final Predicate<Property> test = isMultipleValuedProperty;
        try {
            test.apply(null);
            fail("Null values should throw a NullPointerException");
        } catch (final NullPointerException e) {
        }
        boolean actual = test.apply(mockYes);
        assertEquals(true, actual);
        actual = test.apply(mockNo);
        assertEquals(false, actual);
        when(mockYes.isMultiple()).thenThrow(new RepositoryException());
        try {
            test.apply(mockYes);
            fail("Unexpected completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
    }

    @Test
    public void testIsBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertTrue(isBinaryContentProperty.apply(mockProperty));
    }

    @Test
    public void testIsReferenceProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.REFERENCE);
        when(mockProperty.getName()).thenReturn("foo" + REFERENCE_PROPERTY_SUFFIX);
        assertTrue(isReferenceProperty.apply(mockProperty));
    }
    @Test
    public void testIsReferencePropertyWeak() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.WEAKREFERENCE);
        when(mockProperty.getName()).thenReturn("foo" + REFERENCE_PROPERTY_SUFFIX);
        assertTrue(isReferenceProperty.apply(mockProperty));
    }

    @Test
    public void testIsInternalProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.REFERENCE);
        when(mockProperty.getName()).thenReturn("foo" + REFERENCE_PROPERTY_SUFFIX);
        assertTrue(isInternalProperty.apply(mockProperty));

        when(mockProperty.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertTrue(isInternalProperty.apply(mockProperty));
    }

    @Test
    public void testIsNotBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.STRING);
        assertFalse(isBinaryContentProperty.apply(mockProperty));
    }

    @Test
    public void testContentButNotBinaryContentProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(PropertyType.STRING);
        when(mockProperty.getName()).thenReturn(JcrConstants.JCR_DATA);
        assertFalse(isBinaryContentProperty.apply(mockProperty));
    }

    @Test
    public void testGetPredicateForProperty() throws RepositoryException {
        final PropertyMock mockProp = mock(PropertyMock.class);
        getPredicateForProperty.apply(mockProp);
        when(mockProp.getNamespaceURI()).thenThrow(new RepositoryException());
        try {
            getPredicateForProperty.apply(mockProp);
            fail("Unexpected completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
    }

    @Test
    public void testGetDefinitionForPropertyName() throws RepositoryException {
        final String mockPropertyName = "mock:property";
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        when(mockNTM.getNodeType(anyString())).thenReturn(mockNodeType);
        when(mockPropertyDefinition.getName()).thenReturn(mockPropertyName);
        final PropertyDefinition[] PDs =
                new PropertyDefinition[] {mockPropertyDefinition};
        when(mockNodeType.getPropertyDefinitions()).thenReturn(PDs);
        PropertyDefinition actual =
                getDefinitionForPropertyName(mockNode, mockPropertyName);
        assertEquals(mockPropertyDefinition, actual);
        actual =
                getDefinitionForPropertyName(mockNode, mockPropertyName +
                        ":fail");
        assertNull(actual);

    }

    @Test
    public void testConvertDateToXSDString() {
        final String expected = "2006-11-13T09:40:55.001Z";
        final Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.set(2006, 10, 13, 9, 40, 55);
        date.set(MILLISECOND, 1);
        assertEquals(expected, convertDateToXSDString(date.getTimeInMillis()));
    }

    @Test
    public void testGetVersionHistoryForSessionAndPath() throws RepositoryException {
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/my/path")).thenReturn(
                mockVersionHistory);

        final VersionHistory versionHistory =
                getVersionHistory(mockSession, "/my/path");
        assertEquals(mockVersionHistory, versionHistory);
    }

    @Test
    public void testIsInternalNode() throws RepositoryException {
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.isNodeType("mode:system")).thenReturn(true);
        assertTrue("mode:system nodes should be treated as internal nodes!",
                isInternalNode.apply(mockNode));

        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.isNodeType("mode:system")).thenReturn(false);
        assertFalse("Nodes that are not mode:system types should not be "
                + "treated as internal nodes!", isInternalNode.apply(mockNode));

        when(mockNode.getPrimaryNodeType()).thenThrow(new RepositoryException());
        try {
            isInternalNode.apply(mockNode);
            fail("Unexpected completion of FedoraTypesUtils.isInternalNode" +
                 " after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
    }

    @Test
    public void testPredicateExceptionHandling() throws RepositoryException {
        when(mockNode.getMixinNodeTypes()).thenThrow(new RepositoryException());
        try {
            isFedoraResource.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraResource" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
        try {
            isFedoraObject.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraObject" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
        try {
            isFedoraDatastream.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraDatastream" +
                 " completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
    }

    @Test
    public void testValue2String() throws RepositoryException {
        // test a valid Value
        when(mockValue.getString()).thenReturn("foo");
        assertEquals("foo", value2string.apply(mockValue));
        when(mockValue.getString()).thenThrow(new RepositoryException());
        try {
            value2string.apply(mockValue);
            fail("Unexpected FedoraTypesUtils.value2string" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {
        } // expected
        try {
            value2string.apply(null);
            fail("Unexpected FedoraTypesUtils.value2string" +
                    " completion with null argument!");
        } catch (final NullPointerException e) {
        } // expected
    }

    @Test
    public void testProperty2values() throws RepositoryException {
        // single-valued
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getValue()).thenReturn(mockValue);
        assertEquals("Found wrong Value!", FedoraTypesUtils.property2values
                .apply(mockProperty).next(), mockValue);
        // multi-valued
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getValues()).thenReturn(
                new Value[] {mockValue, mockValue2});
        final Iterator<Value> testIterator = FedoraTypesUtils.property2values.apply(mockProperty);
        assertEquals("Found wrong Value!", testIterator.next(), mockValue);
        assertEquals("Found wrong Value!", testIterator.next(), mockValue2);

    }

    @Test
    public void testPropertyContainsWithNull() throws RepositoryException {
        assertFalse(propertyContains(null, "any-string"));
    }

    @Test
    public void testSingleValuedPropertyContains() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getString()).thenReturn("some-string");
        assertTrue(propertyContains(mockProperty, "some-string"));
        assertFalse(propertyContains(mockProperty, "some-other-string"));
    }

    @Test
    public void testMultiValuedPropertyContains() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getValues()).thenReturn(new Value[] { mockValue });
        when(mockValue.getString()).thenReturn("some-string");
        assertTrue(propertyContains(mockProperty, "some-string"));
        assertFalse(propertyContains(mockProperty, "some-other-string"));
    }

    @Test
    public void testNodeHasTypeNo() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        assertFalse(nodeHasType(mockNode, "mixin"));
    }

    @Test
    public void testNodeHasTypeYes() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        when(mockNTM.hasNodeType("mixin")).thenReturn(true);
        assertTrue(nodeHasType(mockNode, "mixin"));
    }

    @Test
    public void testNodeHasTypeNullMixin() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        assertFalse(nodeHasType(mockNode, null));
    }

    @Test
    public void testNodeHasTypeNull() throws RepositoryException {
        assertFalse(nodeHasType(null, "mixin"));
    }

}
