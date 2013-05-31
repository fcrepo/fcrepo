/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.apache.tika.io.IOUtils;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Feb 21, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "org.slf4j.*",
            "javax.xml.parsers.*",
            "org.apache.xerces.*" })
@PrepareForTest({ FedoraTypesUtils.class })
public class DatastreamTest implements FedoraJcrTypes {

    String testPid = "testObj";

    String testDsId = "testDs";

    Datastream testObj;

    Session mockSession;

    Node mockRootNode;

    Node mockDsNode;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() {

        mockSession = mock(Session.class);
        mockRootNode = mock(Node.class);
        mockDsNode = mock(Node.class);
        final NodeType[] nodeTypes = new NodeType[0];
        try {
            when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
            when(mockDsNode.getName()).thenReturn(testDsId);
            when(mockDsNode.getSession()).thenReturn(mockSession);
            testObj = new Datastream(mockDsNode);
        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @todo Add Documentation.
     */
    @After
    public void tearDown() {
        mockSession = null;
        mockRootNode = null;
        mockDsNode = null;
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetNode() {
        assertEquals(testObj.getNode(), mockDsNode);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetContent() throws RepositoryException, IOException {
        final String expected = "asdf";
        final Node mockContent = TestHelpers.getContentNodeMock(expected);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final String actual = IOUtils.toString(testObj.getContent());
        assertEquals(expected, actual);
        verify(mockContent).getProperty(JCR_DATA);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSetContent() throws RepositoryException,
            InvalidChecksumException {
        final org.modeshape.jcr.api.Binary mockBin =
                mock(org.modeshape.jcr.api.Binary.class);
        final InputStream mockStream = mock(InputStream.class);
        PowerMockito.mockStatic(FedoraTypesUtils.class);
        when(
                FedoraTypesUtils.getBinary(any(Node.class),
                        any(InputStream.class), any(String.class))).thenReturn(
                mockBin);
        final Node mockContent = TestHelpers.getContentNodeMock(8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockVF.createBinary(any(InputStream.class))).thenReturn(mockBin);
        final Property mockData = mock(Property.class);
        when(mockContent.canAddMixin(FEDORA_BINARY)).thenReturn(true);
        when(mockContent.setProperty(JCR_DATA, mockBin)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        when(mockData.getBinary()).thenReturn(mockBin);
        testObj.setContent(mockStream);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void getContentSize() throws RepositoryException {
        final int expectedContentLength = 2;
        final Node mockContent =
                TestHelpers.getContentNodeMock(expectedContentLength);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final long actual = testObj.getContentSize();
        verify(mockContent).getProperty(CONTENT_SIZE);
        assertEquals(expectedContentLength, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void getContentDigest() throws RepositoryException {
        final String content = "asdf";
        final URI expected =
                URI.create(TestHelpers.checksumString(content));
        final Node mockContent = TestHelpers.getContentNodeMock(content);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final URI actual = testObj.getContentDigest();
        assertEquals(expected, actual);
        verify(mockContent).getProperty(CONTENT_DIGEST);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetDsId() throws RepositoryException {
        final String actual = testObj.getDsId();
        assertEquals(testDsId, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetObject() throws RepositoryException {
        final Node mockObjectNode = mock(Node.class);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_OBJECT);
        when(mockObjectNode.getMixinNodeTypes()).thenReturn(new NodeType[] { mockNodeType });
        when(mockDsNode.getParent()).thenReturn(mockObjectNode);
        final FedoraObject actual = testObj.getObject();
        assertNotNull(actual);
        assertEquals(mockObjectNode, actual.getNode());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetMimeType() throws RepositoryException {
        final Node mockContent = TestHelpers.getContentNodeMock(8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockContent.hasProperty(JCR_MIME_TYPE)).thenReturn(true);

        final Property mockProperty = mock(Property.class);
        when(mockContent.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);
        when(mockProperty.getString()).thenReturn("application/x-mime-type");
        assertEquals("application/x-mime-type", testObj.getMimeType());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetMimeTypeWithNoContent() throws RepositoryException {
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(false);
        assertEquals("application/octet-stream", testObj.getMimeType());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetMimeTypeWithDefault() throws RepositoryException {
        final Node mockContent = TestHelpers.getContentNodeMock(8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockContent.hasProperty(JCR_MIME_TYPE)).thenReturn(false);

        assertEquals("application/octet-stream", testObj.getMimeType());
    }

    /**
     * @todo Add Documentation.
     */
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

    /**
     * @todo Add Documentation.
     */
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

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetSize() throws RepositoryException {
        final int expectedProps = 1;
        final int expectedContentLength = 2;
        final Node mockContent =
                TestHelpers.getContentNodeMock(expectedContentLength);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockDsNode.getProperties()).thenAnswer(
                new Answer<PropertyIterator>() {

                    @Override
                    public PropertyIterator answer(
                            final InvocationOnMock invocation) {
                        return TestHelpers
                                .getPropertyIterator(1, expectedProps);
                    }
                });
        final long actual = testObj.getSize();
        verify(mockDsNode, times(1)).getProperties();
        assertEquals(3, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHasMixin() throws RepositoryException {
        final NodeType mockYes = mock(NodeType.class);
        when(mockYes.getName()).thenReturn(FEDORA_DATASTREAM);
        final NodeType mockNo = mock(NodeType.class);
        when(mockNo.getName()).thenReturn("not" + FEDORA_DATASTREAM);
        final NodeType[] types = new NodeType[] {mockYes};
        final Node test = mock(Node.class);
        when(test.getMixinNodeTypes()).thenReturn(types);
        assertEquals(true, Datastream.hasMixin(test));
        types[0] = mockNo;
        assertEquals(false, Datastream.hasMixin(test));
    }
}
