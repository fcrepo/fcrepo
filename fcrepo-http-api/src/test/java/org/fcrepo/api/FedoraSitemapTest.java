/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;

import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraSitemapTest {

    FedoraSitemap testObj;

    ObjectService mockObjects;

    Session mockSession;

    @Before
    public void setUp() throws Exception {
        mockObjects = mock(ObjectService.class);
        testObj = new FedoraSitemap();
        TestHelpers.setField(testObj, "objectService", mockObjects);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetSitemapIndex() throws Exception {
        when(mockObjects.getRepositoryObjectCount()).thenReturn(49999L);

        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(1, sitemapIndex.getSitemapEntries().size());
    }

    @Test
    public void testGetSitemapIndexMultiplePages() throws Exception {
        when(mockObjects.getRepositoryObjectCount()).thenReturn(50001L);

        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(2, sitemapIndex.getSitemapEntries().size());
    }
}
