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
package org.fcrepo.kernel.impl;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.Datastream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ValueFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.fcrepo.kernel.impl.DatastreamImpl.hasMixin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>DatastreamImplTest class.</p>
 *
 * @author ksclarke
 */
public class DatastreamImplTest implements FedoraJcrTypes {

    private static final String testDsId = "testDs";

    private Datastream testObj;

    private String testObjPath;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockDsNode;

    @Mock
    private InputStream mockStream;

    @Mock
    private ValueFactory mockVF;

    @Mock
    private NodeType mockDsNodeType;

    @Before
    public void setUp() {
        initMocks(this);
        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        try {
            when(mockDsNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
            when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
            when(mockDsNode.getName()).thenReturn(testDsId);
            when(mockDsNode.getSession()).thenReturn(mockSession);
            final NodeType mockNodeType = mock(NodeType.class);
            when(mockNodeType.getName()).thenReturn("nt:file");
            when(mockDsNode.getPrimaryNodeType()).thenReturn(mockNodeType);
            testObj = new DatastreamImpl(mockDsNode);
        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        mockSession = null;
        mockRootNode = null;
        mockDsNode = null;
    }

    @Test
    public void testGetNode() {
        assertEquals(testObj.getNode(), mockDsNode);
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Date expected = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(expected);
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
        when(mockDsNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockDsNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        final Date actual = testObj.getCreatedDate();
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        final Date expected = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(expected);
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
        when(mockDsNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockDsNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testHasMixin() throws RepositoryException {
        final NodeType mockYes = mock(NodeType.class);
        when(mockYes.getName()).thenReturn(FEDORA_DATASTREAM);
        final NodeType mockNo = mock(NodeType.class);
        when(mockNo.getName()).thenReturn("not" + FEDORA_DATASTREAM);
        final NodeType[] types = new NodeType[] {mockYes};
        final Node test = mock(Node.class);
        final NodeType mockPrimaryNodeType = mock(NodeType.class);
        when(mockPrimaryNodeType.getName()).thenReturn("nt:object");
        when(test.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(test.getMixinNodeTypes()).thenReturn(types);
        assertEquals(true, hasMixin(test));
        types[0] = mockNo;
        assertEquals(false, hasMixin(test));
    }
}
