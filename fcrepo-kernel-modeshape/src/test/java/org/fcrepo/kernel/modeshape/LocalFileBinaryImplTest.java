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
import static org.fcrepo.kernel.api.FedoraTypes.FILENAME;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static java.util.Collections.singleton;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalFileBinaryImplTest {

    private static final Logger LOGGER = getLogger(LocalFileBinaryImplTest.class);

    private static final String DS_ID = "testDs";

    private static final String EXPECTED_CONTENT = "test content";

    private FedoraBinary testObj;

    private File contentFile;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode, mockDsNode, mockContent, mockParentNode;

    @Mock
    private NodeType mockDsNodeType;

    private String mimeType;

    @Mock
    private Property mimeTypeProperty;

    @Mock
    private Value mockValue;

    @Mock
    private InputStream mockStream;

    @Before
    public void setUp() throws Exception {
        contentFile = File.createTempFile("file", ".txt");
        IOUtils.write(EXPECTED_CONTENT, new FileOutputStream(contentFile));
        mimeType = makeMimeType(contentFile);

        when(mimeTypeProperty.getString()).thenReturn(mimeType);
        when(mockValue.getString()).thenReturn(mimeType);
        when(mimeTypeProperty.getValue()).thenReturn(mockValue);
        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(true);
        when(mockContent.getProperty(HAS_MIME_TYPE)).thenReturn(mimeTypeProperty);

        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        when(mockDsNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
        when(mockDsNode.getName()).thenReturn(DS_ID);
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.getParent()).thenReturn(mockParentNode);

        testObj = new LocalFileBinaryImpl(mockContent);
    }

    @Test
    public void testGetContent() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        final String actual = IOUtils.toString(testObj.getContent());
        assertEquals(EXPECTED_CONTENT, actual);
    }

    @Test
    public void testSetContent() throws Exception {
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
        assertEquals(makeMimeType(contentFile), mimeType);
    }

    @Test
    public void testHasMixin() throws Exception {
        assertTrue(LocalFileBinaryImpl.hasMixin(mockContent));
    }

    @Test
    public void testHasMixinNotBinary() throws Exception {
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(false);
        when(mockContent.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);

        assertFalse(LocalFileBinaryImpl.hasMixin(mockContent));
    }

    @Test
    public void testHasAccessType() throws Exception {
        assertTrue(LocalFileBinaryImpl.hasAccessType(mockContent));
    }

    @Test
    public void testHasAccessTypeNotExternal() throws Exception {
        when(mimeTypeProperty.getString()).thenReturn("text/plain");

        assertFalse(LocalFileBinaryImpl.hasAccessType(mockContent));
    }

    @Test
    public void testHasAccessTypeNotLocalFile() throws Exception {
        when(mimeTypeProperty.getString()).thenReturn(
                "message/external-body; access-type=URL; URL=\"http://example.com/file\"");

        assertFalse(LocalFileBinaryImpl.hasAccessType(mockContent));
    }

    @Test
    public void testHasAccessTypeNoMimeType() throws Exception {
        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(false);

        assertFalse(LocalFileBinaryImpl.hasAccessType(mockContent));
    }

    private String makeMimeType(final File file) {
        return "message/external-body; access-type=LOCAL-FILE; LOCAL-FILE=\"" +
                file.toURI().toString() + "\"";
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
