/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Binary;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BinaryCacheEntryTest {
    @Mock
    private Binary mockBinary;

    @Mock
    private InputStream mockInputStream;

    private BinaryCacheEntry testObj;


    @Before
    public void setUp() {
        initMocks(this);

        testObj = new BinaryCacheEntry(mockBinary, "some-identifier");
    }

    @Test
    public void testGetInputStream() throws Exception {
        when(mockBinary.getStream()).thenReturn(mockInputStream);
        final InputStream actual = testObj.getInputStream();
        assertEquals(mockInputStream, actual);
        verify(mockBinary).getStream();
    }

    @Test
    public void testGetExternalIdentifier() throws Exception {
        assertEquals("some-identifier", testObj.getExternalIdentifier());
    }
}
