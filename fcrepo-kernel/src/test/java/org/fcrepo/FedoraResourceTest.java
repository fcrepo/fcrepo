package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

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
@PowerMockIgnore({"org.apache.xerces.*", "javax.xml.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({NamespaceTools.class, JcrRdfTools.class})
public class FedoraResourceTest {

    FedoraResource testObj;

    Node mockNode;

    Node mockRoot;

    Session mockSession;

    @Before
    public void setUp(){
        mockSession = mock(Session.class);
        mockNode = mock(Node.class);
        testObj = new FedoraResource(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }

    @Test
    public void testPathConstructor() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Node mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getSession()).thenReturn(mockSession);
        FedoraResource test = new FedoraResource(mockSession, "/foo/bar", null);
    }

    @Test
    public void testHasMixin() throws RepositoryException{
        boolean actual = FedoraResource.hasMixin(mockNode);
        assertEquals(false, actual);
        NodeType mockType = mock(NodeType.class);
        NodeType[] mockTypes = new NodeType[]{mockType};
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
        Property mockProp = mock(Property.class);
        Calendar someDate = Calendar.getInstance();
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(mockProp);
        testObj.getCreatedDate();
    }

    @Test
    public void testGetLastModifiedDateDefault() throws RepositoryException{
        // test missing JCR_LASTMODIFIED
        Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockNode.getProperty(FedoraResource.JCR_LASTMODIFIED))
            .thenThrow(RepositoryException.class);
            Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        Date actual = testObj.getLastModifiedDate();
        assertEquals(someDate.getTimeInMillis(), actual.getTime());
        // this is a read operation, it must not persist the session
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetLastModifiedDate(){
        // test existing JCR_LASTMODIFIED
        Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            Property mockProp = mock(Property.class);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.getProperty(FedoraResource.JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        Property mockMod = mock(Property.class);
        Calendar modDate = Calendar.getInstance();
        try{
            when(mockNode.getProperty(FedoraResource.JCR_LASTMODIFIED))
            .thenReturn(mockMod);
            when(mockMod.getDate()).thenReturn(modDate);
        } catch (RepositoryException e) {
            System.err.println("What are we doing in the second test?");
            e.printStackTrace();
        }
        Date actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.getTime());
    }
    
    @Test
    public void testGetGraphProblems() throws RepositoryException {
        Problems actual = testObj.getGraphProblems();
        assertEquals(null, actual);
        JcrPropertyStatementListener mockListener =
                mock(JcrPropertyStatementListener.class);
        setField("listener", FedoraResource.class, mockListener, testObj);
        testObj.getGraphProblems();
        verify(mockListener).getProblems();
    }
    
    private static void setField(String name, Class clazz, Object value, Object object) {
        try{
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
