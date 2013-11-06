/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.utils;

import static java.util.Calendar.MILLISECOND;
import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.convertDateToXSDString;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getDefinitionForPropertyName;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositorySize;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraObject;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isMultipleValuedProperty;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.value2string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_PATH;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.Property;
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
import org.modeshape.jcr.api.Namespaced;

import com.google.common.base.Predicate;

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
    private Value mockValue, mockValue2;

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
            fail("Null values should throw an IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
        }
        boolean actual = test.apply(mockYes);
        assertEquals(true, actual);
        actual = test.apply(mockNo);
        assertEquals(false, actual);
        when(mockYes.isMultiple()).thenThrow(new RepositoryException());
        try {
            test.apply(mockYes);
            fail("Unexpected completion after RepositoryException!");
        } catch (final RuntimeException e) {} // expected
    }

    @Test
    public void testGetPredicateForProperty() throws RepositoryException {
        final PropertyMock mockProp = mock(PropertyMock.class);
        getPredicateForProperty.apply(mockProp);
        when(mockProp.getNamespaceURI()).thenThrow(new RepositoryException());
        try {
            getPredicateForProperty.apply(mockProp);
            fail("Unexpected completion after RepositoryException!");
        } catch (final RuntimeException e) {} // expected
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
        assertEquals(null, actual);

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
    public void testGetBaseVersionForNode() throws RepositoryException {
        when(mockNode.getPath()).thenReturn("/my/path");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getBaseVersion("/my/path")).thenReturn(
                mockVersion);
        final Version versionHistory = getBaseVersion(mockNode);

        assertEquals(mockVersion, versionHistory);
    }

    @Test
    public void testGetVersionHistoryForNode() throws RepositoryException {
        when(mockNode.getPath()).thenReturn("/my/path");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getSession().getWorkspace()).thenReturn(mockWS);
        when(mockWS.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/my/path")).thenReturn(
                mockVersionHistory);

        final VersionHistory versionHistory = getVersionHistory(mockNode);
        assertEquals(mockVersionHistory, versionHistory);
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
        } catch (final RuntimeException e) {} // expected
    }

    @Test
    public void testGetObjectSize() throws RepositoryException {

        when(mockRepository.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getQueryManager()).thenReturn(mockQueryManager);
        when(
                mockQueryManager.createQuery("SELECT [" + CONTENT_SIZE +
                        "] FROM [" + FEDORA_BINARY + "]", JCR_SQL2))
                .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockResults);
        when(mockResults.getRows()).thenReturn(mockIterator);

        when(mockIterator.hasNext()).thenReturn(true, true, true, false);
        when(mockIterator.nextRow()).thenReturn(mockRow, mockRow, mockRow);

        when(mockRow.getValue(CONTENT_SIZE)).thenReturn(mockValue);
        when(mockValue.getLong()).thenReturn(5L, 10L, 1L);

        final long count = getRepositorySize(mockRepository);
        assertEquals("Got wrong count!", 16L, count);
        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetObjectCount() throws RepositoryException {
        when(mockRepository.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getQueryManager()).thenReturn(mockQueryManager);
        when(
                mockQueryManager.createQuery("SELECT [" + JCR_PATH +
                        "] FROM [" + FEDORA_OBJECT + "]", JCR_SQL2))
                .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockResults);
        when(mockResults.getRows()).thenReturn(mockIterator);
        when(mockIterator.getSize()).thenReturn(3L);

        final long count = getRepositoryCount(mockRepository);
        assertTrue(count == 3L);
        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }

    @Test
    public void testPredicateExceptionHandling() throws RepositoryException {
        when(mockNode.getMixinNodeTypes()).thenThrow(new RepositoryException());
        try {
            isFedoraResource.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraResource" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {} // expected
        try {
            isFedoraObject.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraObject" +
                    " completion after RepositoryException!");
        } catch (final RuntimeException e) {} // expected
        try {
            isFedoraDatastream.apply(mockNode);
            fail("Unexpected FedoraTypesUtils.isFedoraDatastream" +
                 " completion after RepositoryException!");
        } catch (final RuntimeException e) {} // expected
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
        } catch (final RuntimeException e) {} // expected
        try {
            value2string.apply(null);
            fail("Unexpected FedoraTypesUtils.value2string" +
                    " completion with null argument!");
        } catch (final IllegalArgumentException e) {} // expected
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
}
