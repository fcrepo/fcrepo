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

import org.apache.tika.io.IOUtils;
import org.fcrepo.kernel.api.FedoraJcrTypes;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ValueFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.fcrepo.kernel.api.FedoraJcrTypes.HAS_MIME_TYPE;

/**
 * <p>DatastreamImplTest class.</p>
 *
 * @author ksclarke
 */
public class FedoraBinaryImplTest implements FedoraJcrTypes {

    private static final String testDsId = "testDs";

    private FedoraBinary testObj;

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

    @Mock
    private Node mockContent;

    @Before
    public void setUp() {
        initMocks(this);
        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        try {
            when(mockDsNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
            when(mockDsNode.getName()).thenReturn(testDsId);
            when(mockContent.getSession()).thenReturn(mockSession);
            final NodeType mockNodeType = mock(NodeType.class);
            when(mockNodeType.getName()).thenReturn("nt:file");
            when(mockDsNode.getPrimaryNodeType()).thenReturn(mockNodeType);
            testObj = new FedoraBinaryImpl(mockContent);
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
        assertEquals(testObj.getNode(), mockContent);
    }

    @Test
    public void testGetContent() throws RepositoryException, IOException {
        final String expected = "asdf";
        getContentNodeMock(mockContent, expected);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final String actual = IOUtils.toString(testObj.getContent());
        assertEquals(expected, actual);
        verify(mockContent).getProperty(JCR_DATA);
    }

    @Test
    public void testGetBinaryContent() throws RepositoryException {
        final Binary mockBinary = mock(Binary.class);
        final Property mockProperty = mock(Property.class);
        getContentNodeMock(mockContent, "abc");
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockProperty);
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        assertEquals(mockBinary, testObj.getBinaryContent());
    }

    @Test
    public void testSetContent() throws RepositoryException,
            InvalidChecksumException {
        final org.modeshape.jcr.api.Binary mockBin =
                mock(org.modeshape.jcr.api.Binary.class);
        getContentNodeMock(mockContent, 8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockVF.createBinary(any(InputStream.class), any(String.class)))
                .thenReturn(mockBin);
        final Property mockData = mock(Property.class);
        when(mockContent.canAddMixin(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.setProperty(JCR_DATA, mockBin)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        when(mockData.getBinary()).thenReturn(mockBin);
        testObj.setContent(mockStream, null, null, null, null);
    }

    @Test
    public void testSetContentWithFilename() throws RepositoryException,
            InvalidChecksumException {
        final org.modeshape.jcr.api.Binary mockBin =
                mock(org.modeshape.jcr.api.Binary.class);
        getContentNodeMock(mockContent, 8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockVF.createBinary(any(InputStream.class), any(String.class)))
                .thenReturn(mockBin);
        final Property mockData = mock(Property.class);
        when(mockContent.canAddMixin(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.setProperty(JCR_DATA, mockBin)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        when(mockData.getBinary()).thenReturn(mockBin);
        testObj.setContent(mockStream, null, null, "xyz", null);
        verify(mockContent).setProperty(FILENAME, "xyz");
    }

    @Test(expected = InvalidChecksumException.class)
    public void testSetContentWithChecksumMismatch()
            throws RepositoryException, InvalidChecksumException,
            URISyntaxException {
        final org.modeshape.jcr.api.Binary mockBin =
                mock(org.modeshape.jcr.api.Binary.class);
        getContentNodeMock(mockContent, 8);
        when(mockDsNode.getSession()).thenReturn(mockSession);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockVF.createBinary(any(InputStream.class), any(String.class)))
                .thenReturn(mockBin);
        final Property mockData = mock(Property.class);
        when(mockContent.canAddMixin(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.setProperty(JCR_DATA, mockBin)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        when(mockData.getBinary()).thenReturn(mockBin);
        testObj.setContent(mockStream, null, new URI("urn:sha1:xyz"), null, null);
    }

    @Test
    public void getContentSize() throws RepositoryException {
        final int expectedContentLength = 2;
        getContentNodeMock(mockContent, expectedContentLength);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final long actual = testObj.getContentSize();
        verify(mockContent).getProperty(CONTENT_SIZE);
        assertEquals(expectedContentLength, actual);
    }

    @Test
    public void getContentDigest() throws RepositoryException {
        final String content = "asdf";
        final URI expected = URI.create(checksumString(content));
        getContentNodeMock(mockContent, content);
        final URI actual = testObj.getContentDigest();
        assertEquals(expected, actual);
        verify(mockContent).getProperty(CONTENT_DIGEST);
    }

    @Test
    public void testGetMimeType() throws RepositoryException {
        getContentNodeMock(mockContent, 8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(true);

        final Property mockProperty = mock(Property.class);
        when(mockContent.getProperty(HAS_MIME_TYPE)).thenReturn(mockProperty);
        when(mockProperty.getString()).thenReturn("application/x-mime-type");
        assertEquals("application/x-mime-type", testObj.getMimeType());
    }

    @Test
    public void testGetMimeTypeWithNoContent() throws RepositoryException {
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(false);
        assertEquals("application/octet-stream", testObj.getMimeType());
    }

    @Test
    public void testGetMimeTypeWithDefault() throws RepositoryException {
        getContentNodeMock(mockContent, 8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(false);

        assertEquals("application/octet-stream", testObj.getMimeType());
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Date expected = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(expected);
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
        when(mockContent.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockContent.getProperty(JCR_CREATED)).thenReturn(mockProp);
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
        when(mockContent.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockContent.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(expected.getTime(), actual.getTime());
    }

}
