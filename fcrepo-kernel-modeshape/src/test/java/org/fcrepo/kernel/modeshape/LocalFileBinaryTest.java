/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FILENAME;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.FedoraTypes.PROXY_FOR;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.apache.tika.io.IOUtils;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static java.util.Collections.singleton;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalFileBinaryTest {

    private static final String DS_ID = "testDs";

    private static final String EXPECTED_CONTENT = "test content";

    private FedoraBinary testObj;

    private File contentFile;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockDsNode, mockContent, mockParentNode;

    @Mock
    private NodeType mockDsNodeType;

    private String mimeType;

    @Mock
    private Property proxyURIProperty;

    @Mock
    private Property mimeTypeProperty;

    @Mock
    private Value mockURIValue;

    @Mock
    private Value mockValue;

    @Mock
    private InputStream mockStream;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    @Before
    public void setUp() throws Exception {
        contentFile = File.createTempFile("file", ".txt");
        IOUtils.write(EXPECTED_CONTENT, new FileOutputStream(contentFile));
        mimeType = "text/plain";


        when(mimeTypeProperty.getString()).thenReturn(mimeType);
        when(mimeTypeProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn(mimeType);

        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(true);
        when(mockContent.getProperty(HAS_MIME_TYPE)).thenReturn(mimeTypeProperty);

        when(proxyURIProperty.toString()).thenReturn(contentFile.toURI().toString());
        when(proxyURIProperty.getString()).thenReturn(contentFile.toURI().toString());
        when(proxyURIProperty.getValue()).thenReturn(mockURIValue);
        when(proxyURIProperty.getName()).thenReturn(PROXY_FOR.toString());

        when(mockURIValue.toString()).thenReturn(contentFile.toURI().toString());
        when(mockURIValue.getString()).thenReturn(contentFile.toURI().toString());

        when(mockContent.hasProperty(PROXY_FOR)).thenReturn(true);
        when(mockContent.getProperty(PROXY_FOR)).thenReturn(proxyURIProperty);

        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        when(mockDsNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
        when(mockDsNode.getName()).thenReturn(DS_ID);
        when(mockContent.getSession()).thenReturn(mockSession);
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.getParent()).thenReturn(mockParentNode);

        testObj = new LocalFileBinary(mockContent);
    }

    @Test
    public void testGetContent() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        final String actual = IOUtils.toString(testObj.getContent());
        assertEquals(EXPECTED_CONTENT, actual);
    }

    @Test
    public void setProxyInfo() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        testObj.setProxyURL(contentFile.toURI().toString());
        verify(mockContent).setProperty(PROXY_FOR, contentFile.toURI().toString());

        assertEquals(contentFile.toURI().toString(), testObj.getProxyURL());
    }

    @Test
    public void testSetContent() throws Exception {
        testObj.setProxyURL(contentFile.toURI().toString());
        verify(mockContent).setProperty(PROXY_FOR, contentFile.toURI().toString());

        testObj.setContent(mockStream, mimeType, null, null, null);
        verify(mockContent).setProperty(HAS_MIME_TYPE, mimeType);
    }

    @Test
    public void testSetContentWithFilename() throws Exception {
        testObj.setContent(mockStream, mimeType, null, contentFile.getName(), null);

        verify(mockContent).setProperty(FILENAME, contentFile.getName());
    }

    @Test
    public void testSetContentWithChecksum() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        testObj.setContent(mockStream, mimeType, singleton(
                new URI(checksum)), contentFile.getName(), null);
    }

    @Test(expected = InvalidChecksumException.class)
    public void testSetContentWithChecksumMismatch() throws Exception {
        testObj.setContent(mockStream, mimeType, singleton(new URI("urn:sha1:xyz")), null, null);
    }

    @Test
    public void getContentSize() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        final long contentSize = testObj.getContentSize();
        assertEquals(12l, contentSize);
    }

    @Test
    public void getProxyInfo() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        final String url = testObj.getProxyURL();
        assertEquals(contentFile.toURI().toString(), url);
    }

    @Test
    public void testGetContentDigest() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockChecksumProperty(checksum);

        final URI digestUri = testObj.getContentDigest();
        assertEquals(checksum, digestUri.toString());
    }

    @Test
    public void testGetMimeType() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);

        final String mimeType = testObj.getMimeType();
        assertEquals("text/plain", mimeType);
    }

    private void mockChecksumProperty(final String checksum) throws Exception {
        when(mockContent.hasProperty(CONTENT_DIGEST)).thenReturn(true);
        final Property checksumProperty = mock(Property.class);
        final Value checksumValue = mock(Value.class);
        when(checksumValue.getString()).thenReturn(checksum);
        when(checksumProperty.getString()).thenReturn(checksum);
        when(checksumProperty.getValue()).thenReturn(checksumValue);
        when(mockContent.getProperty(CONTENT_DIGEST)).thenReturn(checksumProperty);
    }
}
