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
package org.fcrepo.kernel.modeshape.utils.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.modeshape.utils.ExternalResourceCacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
/**
 * <p>CacheEntryFactoryTest class.</p>
 *
 * @author lsitu
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheEntryFactoryTest {

    @Mock
    private Property mockProperty;

    @Mock
    private Value mockValue;

    public static String RESOURCE_URL = "http://www.example.com/file";
    private static String EXTERNAL_RESOURCE = "message/external-body;"
        + " access-type=URL; URL=\"" + RESOURCE_URL + "\"";

    @Before
    public void setUp() throws RepositoryException {
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn(EXTERNAL_RESOURCE);
    }

    @Test
    public void testForProperty() throws RepositoryException {
        final CacheEntry instance = CacheEntryFactory.forProperty(mockProperty);
        assertTrue("CacheEntry class isn't correct", instance instanceof ExternalResourceCacheEntry);
    }
}
