package org.fcrepo.utils;

import static org.fcrepo.utils.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.utils.FedoraJcrTypes.DIGEST_ALGORITHM;
import static org.fcrepo.utils.FedoraJcrTypes.DIGEST_VALUE;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNED;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNERID;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_CREATED;
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

public abstract class TestHelpers {
	public static Node getContentNodeMock(final int size) {
		return getContentNodeMock(randomData(size));
	}

	public static Node getContentNodeMock(final String content) {
		return getContentNodeMock(content.getBytes());
	}

	public static Node getContentNodeMock(final byte[] content) {
		final Node mock = mock(Node.class);
		final long size = content.length;
		final String digest = checksumString(content);
		final String digestType = "SHA-1";
		final Property mockFedoraSize = mock(Property.class);
		final Property mockProp = mock(Property.class);
		final Property mockDigest = mock(Property.class);
		final Property mockDigestType = mock(Property.class);
		final Property mockOwner = mock(Property.class);
		final Property mockCreated = mock(Property.class);
		final Binary mockBin = mock(Binary.class);
		try {
			when(mockFedoraSize.getLong()).thenReturn(size);
			when(mockBin.getSize()).thenReturn(size);
			when(mockBin.getStream()).thenAnswer(new Answer<InputStream>() {

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
			when(mock.getProperty(DIGEST_ALGORITHM)).thenReturn(mockDigestType);
			when(mock.getProperty(DIGEST_VALUE)).thenReturn(mockDigest);
			when(mock.getProperty(FEDORA_OWNERID)).thenReturn(mockOwner);
			when(mock.getProperty(FEDORA_OWNED)).thenReturn(mockOwner);
			when(mock.getProperty(JCR_CREATED)).thenReturn(mockCreated);
		} catch (final RepositoryException e) {
		} // shhh
		return mock;
	}

	public static String checksumString(final String content) {
		return checksumString(content.getBytes());
	}

	public static String checksumString(final byte[] content) {
		try {
			final MessageDigest d = MessageDigest.getInstance("SHA-1");
			final byte[] digest = d.digest(content);
			return ContentDigest.asString(digest);
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

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

	public static byte[] randomData(final int byteLength) {
		final byte[] bytes = new byte[byteLength];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}
}
