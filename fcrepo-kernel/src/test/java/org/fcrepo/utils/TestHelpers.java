/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.fcrepo.utils.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.utils.FedoraJcrTypes.CONTENT_DIGEST;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_CREATEDBY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date May 2, 2013
 */
public abstract class TestHelpers {

    private static final SecureRandom GARBAGE_GENERATOR = new SecureRandom(
            "G4RbAG3".getBytes());

    /**
     * @todo Add Documentation.
     */
    public static Node getContentNodeMock(final int size) {
        return getContentNodeMock(randomData(size));
    }

    /**
     * @todo Add Documentation.
     */
    public static Node getContentNodeMock(final String content) {
        return getContentNodeMock(content.getBytes());
    }

    /**
     * @todo Add Documentation.
     */
    public static Node getContentNodeMock(final byte[] content) {
        final Node mock = mock(Node.class);
        final long size = content.length;
        final String digest = checksumString(content);
        final String digestType = "SHA-1";
        final Property mockFedoraSize = mock(Property.class);
        final Property mockProp = mock(Property.class);
        final Property mockDigest = mock(Property.class);
        final Property mockDigestType = mock(Property.class);
        final Property mockCreated = mock(Property.class);
        final Binary mockBin = mock(Binary.class);
        try {
            when(mockFedoraSize.getLong()).thenReturn(size);
            when(mockBin.getSize()).thenReturn(size);
            when(mockBin.getStream()).thenAnswer(new Answer<InputStream>() {

    /**
     * @todo Add Documentation.
     */
                @Override
                public InputStream answer(final InvocationOnMock inv) {
                    return new ByteArrayInputStream(content);
                }
            });
            when(mockProp.getBinary()).thenReturn(mockBin);
            when(mockDigest.getString()).thenReturn(digest);
            when(mockDigestType.getString()).thenReturn(digestType);
            when(mock.getProperty(JCR_DATA)).thenReturn(mockProp);
            when(mock.getProperty(CONTENT_SIZE)).thenReturn(mockFedoraSize);
            when(mock.getProperty(CONTENT_DIGEST)).thenReturn(mockDigest);
            when(mock.getProperty(JCR_CREATEDBY)).thenReturn(mockCreated);
        } catch (final RepositoryException e) {
        } // shhh
        return mock;
    }

    /**
     * @todo Add Documentation.
     */
    public static String checksumString(final String content) {
        return checksumString(content.getBytes());
    }

    /**
     * @todo Add Documentation.
     */
    public static String checksumString(final byte[] content) {
        try {
            final MessageDigest d = MessageDigest.getInstance("SHA-1");
            final byte[] digest = d.digest(content);
            return ContentDigest.asURI("SHA-1", digest).toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    public static PropertyIterator getPropertyIterator(final int numValues,
            final long size) {
        final PropertyIterator mock = mock(PropertyIterator.class);
        final Property mockProp = mock(Property.class);
        try {
            when(mockProp.isMultiple()).thenReturn(numValues > 1);
            if (numValues != 1) {
                final Value[] values = new Value[numValues];
                for (int i = 0; i < numValues; i++) {
                    final Value mockVal = mock(Value.class);
                    final Binary mockBin = mock(Binary.class);
                    when(mockVal.getBinary()).thenReturn(mockBin);
                    when(mockBin.getSize()).thenReturn(size);
                    values[i] = mockVal;
                }
                when(mockProp.getValues()).thenReturn(values);
            } else {
                final Binary mockBin = mock(Binary.class);
                when(mockBin.getSize()).thenReturn(size);
                when(mockProp.getBinary()).thenReturn(mockBin);
            }
        } catch (final RepositoryException e) {
        } // shhh
        when(mock.getSize()).thenReturn(1L);
        when(mock.hasNext()).thenReturn(true, false);
        when(mock.nextProperty()).thenReturn(mockProp).thenThrow(
                IndexOutOfBoundsException.class);
        return mock;
    }

    /**
     * @todo Add Documentation.
     */
    public static byte[] randomData(final int byteLength) {
        final byte[] bytes = new byte[byteLength];
        GARBAGE_GENERATOR.nextBytes(bytes);
        return bytes;
    }
}
