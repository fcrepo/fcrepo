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
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_SIZE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FILENAME;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.FedoraTypes.PROXY_FOR;
import static org.fcrepo.kernel.api.FedoraTypes.REDIRECTS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.getContentNodeMock;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.checksumString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Collections.singleton;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.net.URI;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.exception.ExternalContentAccessException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UrlBinaryTest {

    private static final String EXPECTED_CONTENT = "test content";

    private FedoraBinary testObj;

    private final String mimeType = "text/plain";

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
    private Value mockURIValue;

    @Mock
    private Node mockDescNode, mockContent, mockParentNode;

    @Mock
    private NodeType mockDescNodeType;

    @Before
    public void setUp() throws Exception {
        final NodeType[] nodeTypes = new NodeType[] { mockDescNodeType };

        stubFor(head(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Length", Long.toString(EXPECTED_CONTENT.length()))
                        .withHeader("Content-Type", "text/plain")));
        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Content-Length", Long.toString(EXPECTED_CONTENT.length()))
                        .withBody(EXPECTED_CONTENT)));
        fileUrl = "http://localhost:" + wireMockRule.port() + "/file.txt";


        when(mockDescNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(mockDescNode.getMixinNodeTypes()).thenReturn(nodeTypes);
        when(mockDescNode.getParent()).thenReturn(mockContent);
        when(mockContent.getSession()).thenReturn(mockSession);
        when(mockContent.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.getParent()).thenReturn(mockParentNode);
        when(mockContent.getNode(FEDORA_DESCRIPTION)).thenReturn(mockDescNode);
        when(mockDescNode.setProperty(anyString(), any(Binary.class))).thenReturn(mockProperty);

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:versionedFile");
        when(mockContent.getPrimaryNodeType()).thenReturn(mockNodeType);

        testObj = new UrlBinary(mockContent);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetContent() throws Exception {
        mockProxyProperty();
        testObj.setContent(null, mimeType, null, null, null);
    }

    @Test
    public void testSetExternalContent() throws Exception {
        mockProxyProperty();
        testObj.setExternalContent(mimeType, null, null, PROXY, "http://example.com");

        verify(mockDescNode).setProperty(CONTENT_SIZE, EXPECTED_CONTENT.length());
        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
        verify(mockContent).setProperty(PROXY_FOR, "http://example.com");
    }

    @Test
    public void testSetContentWithFilename() throws Exception {
        mockProxyProperty();
        final String fileName = "content.txt";
        testObj.setExternalContent(mimeType, null, fileName, PROXY, fileName);

        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
        verify(mockDescNode).setProperty(FILENAME, fileName);
    }

    @Test
    public void testSetExternalContentWithChecksum() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockProxyProperty();

        testObj.setExternalContent(mimeType, singleton(new URI(checksum)), null, PROXY, "http://www.example.com");

        verify(mockDescNode).setProperty(CONTENT_DIGEST, new String[]{checksum});
        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
    }

    @Test
    public void testSetContentWithChecksum() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockProxyProperty();

        testObj.setExternalContent(mimeType, singleton(new URI(checksum)), null, PROXY, "content.txt");

        verify(mockDescNode).setProperty(CONTENT_DIGEST, new String[]{checksum});
        verify(mockDescNode).setProperty(HAS_MIME_TYPE, mimeType);
    }


    @Test(expected = InvalidChecksumException.class)
    public void testSetContentWithChecksumMismatch() throws Exception {
        mockProxyProperty();
        testObj.setExternalContent(mimeType, singleton(new URI("urn:sha1:xyz")), null, PROXY, "content.txt");
    }

    @Test
    public void testGetContentSize() throws Exception {
        mockProxyProperty();
        testObj.setExternalContent(mimeType, null, null, PROXY, "content.txt");

        when(mockProperty.getLong()).thenReturn((long) EXPECTED_CONTENT.length());
        when(mockDescNode.getProperty(CONTENT_SIZE)).thenReturn(mockProperty);
        when(mockDescNode.hasProperty(CONTENT_SIZE)).thenReturn(true);

        final long contentSize = testObj.getContentSize();
        assertEquals(EXPECTED_CONTENT.length(), contentSize);
    }

    @Test
    public void testGetProxyURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockProxyProperty();

        final String url = testObj.getProxyURL();
        assertEquals(fileUrl, url);
    }

    @Test
    public void testSetProxyURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockProxyProperty();

        testObj.setProxyURL(fileUrl);
        verify(mockContent).setProperty(PROXY_FOR, fileUrl);

        assertEquals(fileUrl, testObj.getProxyURL());
    }

    @Test
    public void testGetRedirectURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockRedirectProperty();

        final String url = testObj.getRedirectURL();
        assertEquals(fileUrl, url);
    }

    @Test
    public void testSetRedirectURL() throws Exception {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);
        when(mockDescNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        mockRedirectProperty();

        testObj.setRedirectURL(fileUrl);
        verify(mockContent).setProperty(REDIRECTS_TO, fileUrl);

        assertEquals(fileUrl, testObj.getRedirectURL());
    }

    @Test
    public void testGetContentDigest() throws Exception {
        final String checksum = checksumString(EXPECTED_CONTENT);
        mockProxyProperty();
        mockChecksumProperty(checksum);

        testObj.setExternalContent(mimeType, singleton(new URI(checksum)), null, PROXY, "contents.txt");

        final URI digestUri = testObj.getContentDigest();
        assertEquals(checksum, digestUri.toString());
    }

    @Test
    public void testGetMimeType() {
        getContentNodeMock(mockContent, mockDescNode, EXPECTED_CONTENT);

        final String mimeType = testObj.getMimeType();
        assertEquals(mimeType, mimeType);
    }

    @Test(expected = ExternalContentAccessException.class)
    public void testDisappearingRemoteUri() throws Exception {

        // Wiremock connection reset fault does not work on Windows
        assumeFalse(System.getProperty("os.name").startsWith("Windows"));

        final String remoteHost = "http://localhost:" + wireMockRule.port();
        final String remoteUri = "/resource1";
        stubFor(get(urlEqualTo(remoteUri))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        mockProxyProperty();
        when(proxyURLProperty.getString()).thenReturn(remoteHost + remoteUri);
        when(mockURIValue.getString()).thenReturn(remoteHost + remoteUri);

        when(mockContent.getPath()).thenReturn("/some-fake-path");

        testObj.getContent();
    }

    private void mockProxyProperty() {
        try {
            when(proxyURLProperty.getString()).thenReturn(fileUrl);
            when(proxyURLProperty.getValue()).thenReturn(mockURIValue);
            when(proxyURLProperty.getName()).thenReturn(PROXY_FOR);
            when(mockURIValue.getString()).thenReturn(fileUrl);

            when(mockContent.hasProperty(PROXY_FOR)).thenReturn(true);
            when(mockContent.getProperty(PROXY_FOR)).thenReturn(proxyURLProperty);
        } catch (final RepositoryException e) {
            // This catch left intentionally blank.
        }
    }

    private void mockRedirectProperty() {
        try {
            when(redirectURLProperty.getString()).thenReturn(fileUrl);
            when(redirectURLProperty.getValue()).thenReturn(mockURIValue);
            when(redirectURLProperty.getName()).thenReturn(PROXY_FOR);
            when(mockContent.hasProperty(REDIRECTS_TO)).thenReturn(true);
            when(mockContent.getProperty(REDIRECTS_TO)).thenReturn(redirectURLProperty);
        } catch (final RepositoryException e) {
            // This catch left intentionally blank.
        }

    }

    private void mockChecksumProperty(final String checksum) throws Exception {
        when(mockDescNode.hasProperty(CONTENT_DIGEST)).thenReturn(true);
        final Property checksumProperty = mock(Property.class);
        final Value checksumValue = mock(Value.class);
        when(checksumValue.getString()).thenReturn(checksum);
        when(checksumProperty.getString()).thenReturn(checksum);
        when(checksumProperty.getValue()).thenReturn(checksumValue);
        when(mockDescNode.getProperty(CONTENT_DIGEST)).thenReturn(checksumProperty);
    }
}
