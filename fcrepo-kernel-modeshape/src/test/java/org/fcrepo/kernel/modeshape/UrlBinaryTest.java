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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.fcrepo.kernel.api.FedoraTypes.*;
import static org.fcrepo.kernel.api.FedoraTypes.PROXY_FOR;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.InputStream;
import java.net.URI;
import static java.util.Collections.singleton;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class UrlBinaryTest {

    private static final String DS_ID = "testDs";

    private static final String EXPECTED_CONTENT = "test content";

    private FedoraBinary testObj;

    private String mimeType;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private String fileUrl;

    @Mock
    private Session mockSession;

    @Mock
    private Property mimeTypeProperty;

    @Mock
    private Property proxyURLProperty;

    @Mock
    private Property redirectURLProperty;

    @Mock
    private Property mockProperty;

    @Mock
    private Value mockValue;

    @Mock
    private Node mockDsNode, mockContent, mockParentNode;

    @Mock
    private NodeType mockDsNodeType;

    @Mock
    private InputStream mockStream;

    @Before
    public void setUp() throws Exception {
        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(EXPECTED_CONTENT)));
        fileUrl = "http://localhost:" + wireMockRule.port() + "/file.txt";

        mimeType = "text/plain";

        when(mimeTypeProperty.getString()).thenReturn(mimeType);
        when(mockValue.getString()).thenReturn(mimeType);
        when(mimeTypeProperty.getValue()).thenReturn(mockValue);
        when(mockContent.hasProperty(HAS_MIME_TYPE)).thenReturn(true);
        when(mockContent.getProperty(HAS_MIME_TYPE)).thenReturn(mimeTypeProperty);

        when(proxyURLProperty.getString()).thenReturn(fileUrl);
        when(proxyURLProperty.getValue()).thenReturn(mockValue);
        when(mockContent.hasProperty(PROXY_FOR)).thenReturn(true);
        when(mockContent.getProperty(PROXY_FOR)).thenReturn(proxyURLProperty);

        when(redirectURLProperty.getString()).thenReturn(fileUrl);
        when(redirectURLProperty.getValue()).thenReturn(mockValue);
        when(mockContent.hasProperty(REDIRECTS_TO)).thenReturn(true);
        when(mockContent.getProperty(REDIRECTS_TO)).thenReturn(redirectURLProperty);

        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        when(mockDsNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
        when(mockDsNode.getName()).thenReturn(DS_ID);
        when(mockContent.getSession()).thenReturn(mockSession);
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.getParent()).thenReturn(mockParentNode);
        when(mockContent.setProperty(anyString(), any(Binary.class))).thenReturn(mockProperty);

        testObj = new UrlBinary(mockContent);
    }

    @Test
    public void testSetContent() throws Exception {
        testObj.setContent(mockStream, mimeType, null, null, null);

        verify(mockContent).setProperty(HAS_MIME_TYPE, mimeType);
    }

    @Test
    public void testSetContentWithFilename() throws Exception {
        final String fileName = "content.txt";
        testObj.setContent(mockStream, mimeType, null, fileName, null);

        verify(mockContent).setProperty(HAS_MIME_TYPE, mimeType);
        verify(mockContent).setProperty(FILENAME, fileName);
    }

    @Test
    public void testSetContentWithChecksum() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);

        testObj.setContent(mockStream, mimeType, singleton(
                new URI(checksum)), null, null);
    }

    @Test(expected = InvalidChecksumException.class)
    public void testSetContentWithChecksumMismatch() throws Exception {
        testObj.setContent(mockStream, mimeType, singleton(new URI("urn:sha1:xyz")), null, null);
    }

    @Test
    public void getContentSize() throws Exception {
        testObj.setContent(mockStream, mimeType, null, null, null);

        final long contentSize = testObj.getContentSize();
        assertEquals(-1l, contentSize);
    }

    @Test
    public void testGetProxyURL() throws Exception {
        final String url = testObj.getProxyURL();
        assertEquals(fileUrl, url);
    }
    @Test
    public void testSetProxyURL() throws Exception {
        final String testURL = "http://localhost/fcrepo/rest/foo.txt";
        testObj.setProxyURL(testURL);

        verify(mockContent).setProperty(PROXY_FOR, testURL);
    }

    @Test
    public void testGetRedirectURL() throws Exception {
        final String url = testObj.getRedirectURL();
        assertEquals(fileUrl, url);
    }

    @Test
    public void testSetRedirectURL() throws Exception {
        final String testURL = "http://localhost/fcrepo/rest/foo.txt";
        testObj.setRedirectURL(testURL);

        verify(mockContent).setProperty(REDIRECTS_TO, testURL);
    }

    @Test
    public void testGetContentDigest() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockChecksumProperty(checksum);

        testObj.setContent(mockStream, mimeType, singleton(
                new URI(checksum)), null, null);

        final URI digestUri = testObj.getContentDigest();
        assertEquals(checksum, digestUri.toString());
    }

    @Test
    public void testGetMimeType() throws Exception {
        getContentNodeMock(mockContent, EXPECTED_CONTENT);

        final String mimeType = testObj.getMimeType();
        assertEquals(mimeType, mimeType);
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
