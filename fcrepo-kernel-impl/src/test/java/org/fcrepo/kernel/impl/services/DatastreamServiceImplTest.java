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
package org.fcrepo.kernel.impl.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.utils.CacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.value.binary.StoredBinaryValue;

/**
 * <p>DatastreamServiceImplTest class.</p>
 *
 * @author ksclarke
 */
public class DatastreamServiceImplTest implements FedoraJcrTypes {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private InputStream mockIS;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockDsNodeType;

    @Mock
    private Node mockContent;

    @Mock
    private Property mockProperty;

    @Mock
    private StoredBinaryValue mockBinary;

    private DatastreamService testObj;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private Property mockData;

    @Mock
    private javax.jcr.Binary mockBinaryValue;

    @Mock
    private Repository mockRepository;

    @Mock
    private CacheEntry mockCacheEntry;

    private IdentifierConverter<Resource,Node> mockSubjects;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new DatastreamServiceImpl();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockNode.getSession()).thenReturn(mockSession);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:file");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        mockSubjects = new DefaultIdentifierTranslator(mockSession);
        when(mockNode.getPath()).thenReturn("/some/path");
    }

    @Test
    public void testGetDatastream() throws Exception {
        final String testPath = "/foo/bar";
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockNode.isNodeType(FEDORA_DATASTREAM)).thenReturn(true);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});

        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        testObj.findOrCreateDatastream(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }
}
