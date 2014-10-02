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
package org.fcrepo.http.api;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.jcr.Session;

import org.fcrepo.http.api.repository.FedoraRepositorySitemap;
import org.fcrepo.http.commons.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.kernel.services.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraRepositorySitemapTest class.</p>
 *
 * @author cbeer
 */
public class FedoraRepositorySitemapTest {

    private FedoraRepositorySitemap testObj;

    @Mock
    private RepositoryService mockService;

    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new FedoraRepositorySitemap();
        setField(testObj, "repositoryService", mockService);
        setField(testObj, "uriInfo", getUriInfoImpl());
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetSitemapIndex() {
        when(mockService.getRepositoryObjectCount()).thenReturn(49999L);
        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(1, sitemapIndex.getSitemapEntries().size());
    }

    @Test
    public void testGetSitemapIndexMultiplePages() {
        when(mockService.getRepositoryObjectCount()).thenReturn(50001L);
        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(2, sitemapIndex.getSitemapEntries().size());
    }
}
