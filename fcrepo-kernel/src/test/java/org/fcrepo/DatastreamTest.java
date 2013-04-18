
package org.fcrepo;

import static org.fcrepo.TestHelpers.checksumString;
import static org.fcrepo.TestHelpers.getContentNodeMock;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
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
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.tika.io.IOUtils;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FedoraTypesUtils.class})
public class DatastreamTest implements FedoraJcrTypes {

    String testPid = "testObj";

    String testDsId = "testDs";

    Datastream testObj;

    Session mockSession;

    Node mockRootNode;

    Node mockDsNode;

    @Before
    public void setUp() {
        final String relPath =
                getDatastreamJcrNodePath(testPid, testDsId).substring(1);

        mockSession = mock(Session.class);
        mockRootNode = mock(Node.class);
        mockDsNode = mock(Node.class);
        try {
            when(mockDsNode.getName()).thenReturn(testDsId);
            when(mockDsNode.getSession()).thenReturn(mockSession);
            when(mockSession.getRootNode()).thenReturn(mockRootNode);
            when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
            testObj = new Datastream(mockSession, "testObj", "testDs");
            verify(mockRootNode).getNode(relPath);
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
    public void testGetContent() throws RepositoryException, IOException {
        final String expected = "asdf";
        final Node mockContent = getContentNodeMock(expected);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final String actual = IOUtils.toString(testObj.getContent());
        assertEquals(expected, actual);
        verify(mockDsNode).getNode(JCR_CONTENT);
        verify(mockContent).getProperty(JCR_DATA);
    }

    @Test
    public void testSetContent() throws RepositoryException,
            InvalidChecksumException {
        final org.modeshape.jcr.api.Binary mockBin =
                mock(org.modeshape.jcr.api.Binary.class);
        PowerMockito.mockStatic(FedoraTypesUtils.class);
        when(
                FedoraTypesUtils.getBinary(any(Node.class),
                        any(InputStream.class))).thenReturn(mockBin);
        final InputStream content = mock(InputStream.class);
        final Node mockContent = getContentNodeMock(8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockVF.createBinary(any(InputStream.class))).thenReturn(mockBin);
        final Property mockSize = mock(Property.class);
        when(mockContent.setProperty(JCR_DATA, mockBin)).thenReturn(mockSize);
        testObj.setContent(content);
    }

    @Test
    public void getContentSize() throws RepositoryException {
        final int expectedContentLength = 2;
        final Node mockContent = getContentNodeMock(expectedContentLength);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final long actual = testObj.getContentSize();
        verify(mockDsNode).getNode(JCR_CONTENT);
        verify(mockContent).getProperty(CONTENT_SIZE);
        assertEquals(expectedContentLength, actual);
    }

    @Test
    public void getContentDigest() throws RepositoryException {
        final String content = "asdf";
        final URI expected = URI.create("urn:sha1:" + checksumString(content));
        final Node mockContent = getContentNodeMock(content);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final URI actual = testObj.getContentDigest();
        assertEquals(expected, actual);
        verify(mockContent).getProperty(DIGEST_ALGORITHM);
        verify(mockContent).getProperty(DIGEST_VALUE);
    }

    @Test
    public void getContentDigestType() throws RepositoryException {
        final String expected = "SHA-1";
        final Node mockContent = getContentNodeMock(8);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final String actual = testObj.getContentDigestType();
        assertEquals(expected, actual);
        verify(mockContent).getProperty(DIGEST_ALGORITHM);
    }

    @Test
    public void testGetDsId() throws RepositoryException {
        final String actual = testObj.getDsId();
        assertEquals(testDsId, actual);
    }

    @Test
    public void testGetObject() throws RepositoryException {
        final Node mockObjectNode = mock(Node.class);
        when(mockDsNode.getParent()).thenReturn(mockObjectNode);
        final FedoraObject actual = testObj.getObject();
        assertNotNull(actual);
        assertEquals(actual.getNode(), mockObjectNode);
        verify(mockDsNode).getParent();
    }

    @Test
    public void testGetMimeType() throws RepositoryException {
        testObj.getMimeType();
        verify(mockDsNode).hasProperty(FEDORA_CONTENTTYPE);
    }

    @Test
    public void testGetLabel() throws RepositoryException {
        testObj.getLabel();
        verify(mockDsNode).hasProperty(DC_TITLE);
    }

    @Test
    public void testSetLabel() throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        final String expected = "foo";
        testObj.setLabel(expected);
        verify(mockDsNode).setProperty(DC_TITLE, expected);
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Date expected = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(expected);
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
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
        when(mockDsNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testPurge() throws RepositoryException {
        testObj.purge();
        verify(mockDsNode).remove();
    }

    @Test
    public void testGetSize() throws RepositoryException {
        final int expectedProps = 1;
        final int expectedContentLength = 2;
        final Node mockContent = getContentNodeMock(expectedContentLength);
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
        verify(mockDsNode, times(1)).getNode(JCR_CONTENT);
        verify(mockDsNode, times(1)).getProperties();
        assertEquals(3, actual);
    }
}
