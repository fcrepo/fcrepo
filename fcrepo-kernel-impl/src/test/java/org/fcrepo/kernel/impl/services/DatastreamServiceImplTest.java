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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.net.URI;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.utils.FixityResult;
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

    private static final String MOCK_CONTENT_TYPE = "application/test-data";

    private static final String JCR_CONTENT = "jcr:content";

    private static final String JCR_DATA = "jcr:data";

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

    @Test
    public void testGetFixityResultsModel() throws Exception {

        final FixityResult fixityResult = mock(FixityResult.class);
        when(fixityResult.matches(any(Long.class), any(URI.class))).thenReturn(true);
        when(fixityResult.getStoreIdentifier()).thenReturn("mockIdentifier");
        final URI checksumURI = new URI("xyz");
        when(fixityResult.getComputedChecksum()).thenReturn(checksumURI);
        when(fixityResult.getComputedSize()).thenReturn(15L);
        final ImmutableSet<FixityResult> fixityResults = ImmutableSet.of(fixityResult);

        final FedoraBinary mockFedoraBinary = mock(FedoraBinary.class);

        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockProperty);
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockFedoraBinary.getFixity("SHA-1")).thenReturn(fixityResults);

        when(mockFedoraBinary.getNode()).thenReturn(mockNode);
        when(mockFedoraBinary.getContentDigest()).thenReturn(
                new URI("urn:sha1:abc"));


        final Model actual = testObj.getFixityResultsModel(mockSubjects, mockFedoraBinary).asModel();

        assertTrue("Should have a fixity result",
                actual.contains(mockSubjects.toDomain("/some/path"), RdfLexicon.HAS_FIXITY_RESULT, (RDFNode)null));
        assertTrue("Should have the message digest",
                actual.contains(null,
                        RdfLexicon.HAS_MESSAGE_DIGEST,
                        createResource(checksumURI.toString())));
        assertTrue("Should have the size",
                actual.contains(null, RdfLexicon.HAS_SIZE, createTypedLiteral(15L)));
    }


}
