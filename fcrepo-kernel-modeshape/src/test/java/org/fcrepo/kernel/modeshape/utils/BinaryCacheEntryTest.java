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
package org.fcrepo.kernel.modeshape.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>BinaryCacheEntryTest class.</p>
 *
 * @author fasseg
 */
public class BinaryCacheEntryTest {

    @Mock
    private Property mockProperty;

    @Mock
    private Binary mockBinary;

    @Mock
    private InputStream mockInputStream;

    private BinaryCacheEntry testObj;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockProperty.getPath()).thenReturn("/some/path");

        testObj = new BinaryCacheEntry(mockProperty);
    }

    @Test
    public void testGetInputStream() throws Exception {
        when(mockBinary.getStream()).thenReturn(mockInputStream);
        try (final InputStream actual = testObj.getInputStream()) {
            assertEquals(mockInputStream, actual);
            verify(mockBinary).getStream();
        }
    }

    @Test
    public void testGetExternalIdentifier() throws Exception {
        assertEquals("/some/path", testObj.getExternalIdentifier());
    }
}
