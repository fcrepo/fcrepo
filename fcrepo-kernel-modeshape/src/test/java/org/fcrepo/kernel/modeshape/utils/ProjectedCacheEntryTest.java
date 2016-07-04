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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * <p>ProjectedCacheEntryTest class.</p>
 *
 * @author fasseg
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectedCacheEntryTest {

    @Mock
    private Property mockProperty;

    @Mock
    private Binary mockBinary;

    private ProjectedCacheEntry testObj;

    @Before
    public void setUp() throws RepositoryException {
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockProperty.getPath()).thenReturn("/some/path");

        testObj = new ProjectedCacheEntry(mockProperty);
    }

    @Test
    public void testGetExternalIdentifier() {
        final String expected = "/org.modeshape.connector.filesystem.FileSystemConnector:projections:/some/path";

        assertEquals(expected, testObj.getExternalIdentifier());
    }
}
