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
package org.fcrepo.kernel.modeshape.utils;

import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_SIZE;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATEDBY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.kernel.api.utils.ContentDigest;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Helpers to make writing unit tests easier (by providing some generic
 * mocks)
 * @author fasseg
 * @since May 2, 2013
 */
public abstract class TestHelpers {

    private static final SecureRandom GARBAGE_GENERATOR = new SecureRandom(
            "G4RbAG3".getBytes());

    public static Node getContentNodeMock(final Node mock, final Node mockDesc, final int size) {
        return getContentNodeMock(mock, mockDesc, randomData(size));
    }

    public static Node getContentNodeMock(final Node mock, final Node mockDesc, final String content) {
        return getContentNodeMock(mock, mockDesc, content.getBytes());
    }

    public static Node getContentNodeMock(final Node mock, final Node mockDesc, final byte[] content) {
        final long size = content.length;
        final String digest = checksumString(content);
        final Value digestValue = mock(Value.class);
        final Value[] digestArray = {digestValue};
        final String digestType = SHA1.algorithm;
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

                @Override
                public InputStream answer(final InvocationOnMock inv) {
                    return new ByteArrayInputStream(content);
                }
            });
            when(mockProp.getBinary()).thenReturn(mockBin);
            when(mockDigest.getValues()).thenReturn(digestArray);
            when(mockDigestType.getString()).thenReturn(digestType);
            when(mock.hasProperty(JCR_DATA)).thenReturn(true);
            when(mockDesc.hasProperty(CONTENT_SIZE)).thenReturn(true);
            when(mockDesc.hasProperty(CONTENT_DIGEST)).thenReturn(true);
            when(mockDesc.hasProperty(JCR_CREATEDBY)).thenReturn(true);
            when(mock.getProperty(JCR_DATA)).thenReturn(mockProp);
            when(mockDesc.getProperty(CONTENT_SIZE)).thenReturn(mockFedoraSize);
            when(mockDesc.getProperty(CONTENT_DIGEST)).thenReturn(mockDigest);
            when(mockDigest.isMultiple()).thenReturn(true);
            when(digestValue.getString()).thenReturn(digest);
            when(mock.getProperty(JCR_CREATEDBY)).thenReturn(mockCreated);
        } catch (final RepositoryException e) {
        } // shhh
        return mock;
    }

    public static String checksumString(final String content) {
        return checksumString(content.getBytes());
    }

    public static String checksumString(final byte[] content) {
        try {
            final MessageDigest d = MessageDigest.getInstance(SHA1.algorithm);
            final byte[] digest = d.digest(content);
            return ContentDigest.asURI(SHA1.algorithm, digest).toString();
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
        GARBAGE_GENERATOR.nextBytes(bytes);
        return bytes;
    }


    /**
     * Set a field via reflection
     *
     * @param parent the owner object of the field
     * @param name the name of the field
     * @param obj the value to set
     */
    public static void setField(final Object parent, final String name,
        final Object obj) {
        /* check the parent class too if the field could not be found */
        try {
            final Field f = findField(parent.getClass(), name);
            f.setAccessible(true);
            f.set(parent, obj);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static Field findField(final Class<?> clazz, final String name)
            throws NoSuchFieldException {
        for (final Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException("Field " + name +
                                                   " could not be found");
        }
        return findField(clazz.getSuperclass(), name);
    }
}
