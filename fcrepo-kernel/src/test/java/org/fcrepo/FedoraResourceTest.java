
package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
// PowerMock needs to ignore some packages to prevent class-cast errors
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
        "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({NamespaceTools.class, JcrRdfTools.class,
        FedoraTypesUtils.class})
public class FedoraResourceTest {

    FedoraResource testObj;

    Node mockNode;

    Node mockRoot;

    Session mockSession;

    @Before
    public void setUp() {
        mockSession = mock(Session.class);
        mockNode = mock(Node.class);
        testObj = new FedoraResource(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }

    @Test
    public void testPathConstructor() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final Node mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getSession()).thenReturn(mockSession);
        new FedoraResource(mockSession, "/foo/bar", null);
    }

    @Test
    public void testHasMixin() throws RepositoryException {
        boolean actual = FedoraResource.hasMixin(mockNode);
        assertEquals(false, actual);
        final NodeType mockType = mock(NodeType.class);
        final NodeType[] mockTypes = new NodeType[] {mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        actual = FedoraResource.hasMixin(mockNode);
        assertEquals(false, actual);
        when(mockType.getName()).thenReturn(FedoraResource.FEDORA_RESOURCE);
        actual = FedoraResource.hasMixin(mockNode);
        assertEquals(true, actual);
    }

    @Test
    public void testHasContent() throws RepositoryException {
        testObj.hasContent();
        verify(mockNode).hasNode(JcrConstants.JCR_CONTENT);
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Property mockProp = mock(Property.class);
        final Calendar someDate = Calendar.getInstance();
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(true);
        when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                mockProp);
        assertEquals(someDate.getTimeInMillis(), testObj.getCreatedDate().getTime());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastModifiedDateDefault() throws RepositoryException {
        // test missing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockNode.hasProperty(FedoraResource.JCR_LASTMODIFIED))
                    .thenReturn(false);
            final Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(someDate.getTimeInMillis(), actual.getTime());
        // this is a read operation, it must not persist the session
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        // test existing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            final Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(FedoraResource.JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(
                    mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        try {
            when(mockNode.hasProperty(FedoraResource.JCR_LASTMODIFIED)).thenReturn(true);
            when(mockNode.getProperty(FedoraResource.JCR_LASTMODIFIED))
                    .thenReturn(mockMod);
            when(mockMod.getDate()).thenReturn(modDate);
        } catch (final RepositoryException e) {
            System.err.println("What are we doing in the second test?");
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.getTime());
    }

    @Test
    public void testGetGraphProblems() throws RepositoryException {
        final Problems actual = testObj.getGraphProblems();
        assertEquals(null, actual);
        final JcrPropertyStatementListener mockListener =
                mock(JcrPropertyStatementListener.class);
        setField("listener", FedoraResource.class, mockListener, testObj);
        testObj.getGraphProblems();
        verify(mockListener).getProblems();
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        mockStatic(FedoraTypesUtils.class);
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        when(FedoraTypesUtils.getBaseVersion(mockNode)).thenReturn(mockVersion);
        when(FedoraTypesUtils.getVersionHistory(mockNode)).thenReturn(
                mockVersionHistory);

        testObj.addVersionLabel("v1.0.0");
        verify(mockVersionHistory).addVersionLabel("uuid", "v1.0.0", true);
    }

    private static void setField(final String name, final Class<?> clazz,
            final Object value, final Object object) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(object, value);
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }
}
