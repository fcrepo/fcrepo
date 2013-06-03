/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.fcrepo.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Calendar;
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

import org.junit.Test;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Namespaced;

import com.google.common.base.Predicate;

/**
 * @todo Add Documentation.
 Benjamin Armintor * @author
 * @date May 13, 2013
 */
public class FedoraTypesUtilsTest {

    // unfortunately, we need to be able to cast to two interfaces to perform
    // some tests this testing interface allows mocks to do that
    static interface PropertyMock extends Property, Namespaced {
    };

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testIsMultipleValuedProperty() throws RepositoryException {
        final Property mockYes = mock(Property.class);
        when(mockYes.isMultiple()).thenReturn(true);
        final Property mockNo = mock(Property.class);
        final Predicate<Property> test =
            FedoraTypesUtils.isMultipleValuedProperty;
        try {
            test.apply(null);
            fail("Null values should throw an IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
        }
        boolean actual = test.apply(mockYes);
        assertEquals(true, actual);
        actual = test.apply(mockNo);
        assertEquals(false, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetValueFactory() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final Session mockSession = mock(Session.class);
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        final ValueFactory actual =
            FedoraTypesUtils.getValueFactory.apply(mockNode);
        assertEquals(mockVF, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetPredicateForProperty() {
        final PropertyMock mockProp = mock(PropertyMock.class);
        getPredicateForProperty.apply(mockProp);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetBinary() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final Session mockSession = mock(Session.class);
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        final InputStream mockInput = mock(InputStream.class);
        FedoraTypesUtils.getBinary(mockNode, mockInput);
        verify(mockVF).createBinary(mockInput);
        // try it with hints
        final JcrValueFactory mockJVF = mock(JcrValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockJVF);
        final String mockHint = "storage-hint";
        FedoraTypesUtils.getBinary(mockNode, mockInput, mockHint);
        verify(mockJVF).createBinary(mockInput, mockHint);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetDefinitionForPropertyName() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final String mockPropertyName = "mock:property";
        final Session mockSession = mock(Session.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        final Workspace mockWS = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        final NodeTypeManager mockNTM = mock(NodeTypeManager.class);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        final NodeType mockType = mock(NodeType.class);
        when(mockNTM.getNodeType(anyString())).thenReturn(mockType);
        final PropertyDefinition mockPD = mock(PropertyDefinition.class);
        when(mockPD.getName()).thenReturn(mockPropertyName);
        final PropertyDefinition[] PDs = new PropertyDefinition[] {mockPD};
        when(mockType.getPropertyDefinitions()).thenReturn(PDs);
        PropertyDefinition actual =
            FedoraTypesUtils.getDefinitionForPropertyName(mockNode,
                                                          mockPropertyName);
        assertEquals(mockPD, actual);
        actual =
            FedoraTypesUtils.getDefinitionForPropertyName(mockNode,
                                                          mockPropertyName + ":fail");
        assertEquals(null, actual);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testConvertDateToXSDString() {
        final String expected = "2006-11-13T09:40:55.001Z";
        final Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.set(2006, 10, 13, 9, 40, 55);
        date.set(Calendar.MILLISECOND, 1);
        assertEquals(expected, FedoraTypesUtils.convertDateToXSDString(date
                                                                       .getTimeInMillis()));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetBaseVersionForNode() throws RepositoryException {
        final Version mockVersion = mock(Version.class);
        final Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/my/path");

        when(mockNode.getSession()).thenReturn(mock(Session.class));
        when(mockNode.getSession().getWorkspace()).thenReturn(
                                                              mock(Workspace.class));
        when(mockNode.getSession().getWorkspace().getVersionManager())
            .thenReturn(mock(VersionManager.class));
        when(
             mockNode.getSession().getWorkspace().getVersionManager()
             .getBaseVersion("/my/path")).thenReturn(mockVersion);

        final Version versionHistory =
            FedoraTypesUtils.getBaseVersion(mockNode);

        assertEquals(mockVersion, versionHistory);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetVersionHistoryForNode() throws RepositoryException {
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/my/path");

        when(mockNode.getSession()).thenReturn(mock(Session.class));
        when(mockNode.getSession().getWorkspace()).thenReturn(
                                                              mock(Workspace.class));
        when(mockNode.getSession().getWorkspace().getVersionManager())
            .thenReturn(mock(VersionManager.class));
        when(
             mockNode.getSession().getWorkspace().getVersionManager()
             .getVersionHistory("/my/path")).thenReturn(
                                                        mockVersionHistory);

        final VersionHistory versionHistory =
            FedoraTypesUtils.getVersionHistory(mockNode);

        assertEquals(mockVersionHistory, versionHistory);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetVersionHistoryForSessionAndPath()
        throws RepositoryException {
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Session mockSession = mock(Session.class);

        when(mockSession.getWorkspace()).thenReturn(mock(Workspace.class));
        when(mockSession.getWorkspace().getVersionManager()).thenReturn(
                                                                        mock(VersionManager.class));
        when(
             mockSession.getWorkspace().getVersionManager()
             .getVersionHistory("/my/path")).thenReturn(
                                                        mockVersionHistory);

        final VersionHistory versionHistory =
            FedoraTypesUtils.getVersionHistory(mockSession, "/my/path");

        assertEquals(mockVersionHistory, versionHistory);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testInternalNode() throws RepositoryException {
        Node mockNode = mock(Node.class);
        NodeType mockNodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        when(mockNodeType.isNodeType("mode:system")).thenReturn(true);

        assertTrue("mode:system nodes should be treated as internal nodes",
                   FedoraTypesUtils.isInternalNode.apply(mockNode));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testNonInternalNode() throws RepositoryException {
        Node mockNode = mock(Node.class);
        NodeType mockNodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        when(mockNodeType.isNodeType("mode:system")).thenReturn(false);

        assertFalse("nodes that are not mode:system types should not be " +
                    "treated as internal nodes",
                    FedoraTypesUtils.isInternalNode.apply(mockNode));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetObjectSize() throws RepositoryException {

        Repository mockRepository = mock(Repository.class);
        Session mockSession = mock(Session.class);
        Workspace mockWorkspace = mock(Workspace.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        Query mockQuery = mock(Query.class);
        QueryResult mockResults = mock(QueryResult.class);
        RowIterator mockIterator = mock(RowIterator.class);

        Row mockRow = mock(Row.class);

        Value mockValue = mock(Value.class);

        when(mockRepository.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);

        when(mockQueryManager.createQuery("SELECT [" +
                                          FedoraJcrTypes.CONTENT_SIZE +
                                          "] FROM [" +
                                          FedoraJcrTypes.FEDORA_BINARY + "]",
                                          Query.JCR_SQL2))
            .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockResults);
        when(mockResults.getRows()).thenReturn(mockIterator);

        when(mockIterator.hasNext()).thenReturn(true, true, true, false);
        when(mockIterator.nextRow()).thenReturn(mockRow, mockRow, mockRow);

        when(mockRow.getValue(FedoraJcrTypes.CONTENT_SIZE)).thenReturn(mockValue);
        when(mockValue.getLong()).thenReturn(5L, 10L, 1L);

        final long count = FedoraTypesUtils.getRepositorySize(mockRepository);

        assertTrue(count == 16L);

        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetObjectCount() throws RepositoryException {
        Repository mockRepository = mock(Repository.class);
        Session mockSession = mock(Session.class);
        Workspace mockWorkspace = mock(Workspace.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        Query mockQuery = mock(Query.class);
        QueryResult mockResults = mock(QueryResult.class);
        RowIterator mockIterator = mock(RowIterator.class);


        when(mockRepository.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);

        when(mockQueryManager.createQuery("SELECT [" +
                                          JcrConstants.JCR_PATH +
                                          "] FROM [" +
                                          FedoraJcrTypes.FEDORA_OBJECT +
                                          "]", Query.JCR_SQL2))
            .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockResults);
        when(mockResults.getRows()).thenReturn(mockIterator);

        when(mockIterator.getSize()).thenReturn(3L);

        final long count = FedoraTypesUtils.getRepositoryCount(mockRepository);

        assertTrue(count == 3L);

        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }
}
