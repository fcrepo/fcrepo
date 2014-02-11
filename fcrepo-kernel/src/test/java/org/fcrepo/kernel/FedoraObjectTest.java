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

package org.fcrepo.kernel;

import static org.fcrepo.kernel.services.ServiceHelpers.getObjectSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Calendar;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.services.ServiceHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Predicate;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class FedoraObjectTest implements FedoraJcrTypes {

    private static final String testPid = "testObj";

    private static final String mockUser = "mockUser";

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockObjNode;

    @Mock
    private Property mockProp;

    @Mock
    private Predicate<Node> mockPredicate;

    private FedoraObject testFedoraObject;

    private NodeType[] mockNodetypes;

    @Before
    public void setUp() {
        initMocks(this);
        final String relPath = "/" + testPid;
        final NodeType[] types = new NodeType[0];
        try {
            when(mockObjNode.getName()).thenReturn(testPid);
            when(mockObjNode.getSession()).thenReturn(mockSession);
            when(mockObjNode.getMixinNodeTypes()).thenReturn(types);
            final NodeType mockNodeType = mock(NodeType.class);
            when(mockNodeType.getName()).thenReturn("nt:folder");
            when(mockObjNode.getPrimaryNodeType()).thenReturn(mockNodeType);
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

    @After
    public void tearDown() {
        mockSession = null;
        mockRootNode = null;
        mockObjNode = null;
    }

    @Test
    public void testGetName() throws RepositoryException {
        assertEquals(testFedoraObject.getName(), testPid);
    }

    @Test
    public void testGetNode() {
        assertEquals(testFedoraObject.getNode(), mockObjNode);
    }

    @Test
    public void testGetCreated() throws RepositoryException {
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        when(mockObjNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        testFedoraObject.getCreatedDate();
        verify(mockObjNode).getProperty(JCR_CREATED);
    }

    @Test
    public void testGetLastModified() throws RepositoryException {
        when(mockObjNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        testFedoraObject.getLastModifiedDate();
        verify(mockObjNode).getProperty(JCR_LASTMODIFIED);
    }

    @Test
    public void testGetSize() throws RepositoryException {
        mockStatic(ServiceHelpers.class);
        // obviously not a real value
        when(getObjectSize(mockObjNode)).thenReturn(-8L);
        final long actual = testFedoraObject.getSize();
        assertEquals(-8, actual);
    }

    @Test
    public void testGetModels() throws RepositoryException {
        final Collection<String> actual = testFedoraObject.getModels();
        assertNotNull(actual);
    }

}
