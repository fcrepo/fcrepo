/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Collection;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.services.ServiceHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Predicate;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Feb 28, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class FedoraObjectTest implements FedoraJcrTypes {

    String testPid = "testObj";

    String mockUser = "mockUser";

    Session mockSession;

    Node mockRootNode;

    Node mockObjNode;

    FedoraObject testFedoraObject;

    NodeType[] mockNodetypes;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws LoginException, RepositoryException {
        final String relPath = "/" + testPid;

        mockSession = mock(Session.class);
        mockRootNode = mock(Node.class);
        mockObjNode = mock(Node.class);
        NodeType[] types = new NodeType[0];
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockPredicate = mock(Predicate.class);

        try {

            when(mockObjNode.getName()).thenReturn(testPid);
            when(mockObjNode.getSession()).thenReturn(mockSession);
            when(mockObjNode.getMixinNodeTypes()).thenReturn(types);
            when(mockSession.getRootNode()).thenReturn(mockRootNode);
            when(mockRootNode.getNode(relPath)).thenReturn(mockObjNode);
            when(mockSession.getUserID()).thenReturn(mockUser);
            testFedoraObject = new FedoraObject(mockObjNode);

            mockNodetypes = new NodeType[2];
            mockNodetypes[0] = mock(NodeType.class);
            mockNodetypes[1] = mock(NodeType.class);

            when(mockObjNode.getMixinNodeTypes()).thenReturn(mockNodetypes);

            when(mockPredicate.apply(mockObjNode)).thenReturn(true);

        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    /**
     * @todo Add Documentation.
     */
    @After
    public void tearDown() {
        mockSession = null;
        mockRootNode = null;
        mockObjNode = null;
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetName() throws RepositoryException {
        assertEquals(testFedoraObject.getName(), testPid);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetNode() {
        assertEquals(testFedoraObject.getNode(), mockObjNode);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetCreated() throws RepositoryException {
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        when(mockObjNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        testFedoraObject.getCreatedDate();
        verify(mockObjNode).getProperty(JCR_CREATED);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetLastModified() throws RepositoryException {
        final Property mockProp = mock(Property.class);
        when(mockObjNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        testFedoraObject.getLastModifiedDate();
        verify(mockObjNode).getProperty(JCR_LASTMODIFIED);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetSize() throws RepositoryException {
        PowerMockito.mockStatic(ServiceHelpers.class);
        // obviously not a real value
        when(ServiceHelpers.getObjectSize(mockObjNode)).thenReturn(-8L);
        final long actual = testFedoraObject.getSize();
        assertEquals(-8, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetModels() throws RepositoryException {
        final Collection<String> actual = testFedoraObject.getModels();
        assertNotNull(actual);
    }

}
