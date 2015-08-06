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
package org.fcrepo.kernel.modeshape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Calendar;
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.FedoraJcrTypes;
import org.fcrepo.kernel.api.models.Container;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraObjectImplTest class.</p>
 *
 * @author ksclarke
 */
public class ContainerImplTest implements FedoraJcrTypes {

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

    private Container testContainer;

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
            testContainer = new ContainerImpl(mockObjNode);

            mockNodetypes = new NodeType[2];
            mockNodetypes[0] = mock(NodeType.class);
            when(mockNodetypes[0].getName()).thenReturn("some:type");
            mockNodetypes[1] = mock(NodeType.class);
            when(mockObjNode.isNodeType("some:type")).thenReturn(true);

            when(mockObjNode.getMixinNodeTypes()).thenReturn(mockNodetypes);

            when(mockPredicate.test(mockObjNode)).thenReturn(true);

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
    public void testGetNode() {
        assertEquals(testContainer.getNode(), mockObjNode);
    }

    @Test
    public void testGetCreated() throws RepositoryException {
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        when(mockObjNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        testContainer.getCreatedDate();
        verify(mockObjNode).getProperty(JCR_CREATED);
    }

    @Test
    public void testGetLastModified() throws RepositoryException {
        when(mockObjNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockObjNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        when(mockProp.getDate()).thenReturn(Calendar.getInstance());
        testContainer.getLastModifiedDate();
        verify(mockObjNode).getProperty(JCR_LASTMODIFIED);
    }

    @Test
    public void testHasType() {
        assertTrue(testContainer.hasType("some:type"));
    }

}
