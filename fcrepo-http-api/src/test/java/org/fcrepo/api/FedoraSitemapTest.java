package org.fcrepo.api;

import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FedoraSitemapTest {

    FedoraSitemap testObj;

    ObjectService mockObjects;


    Repository mockRepo;

    Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(ObjectService.class);
        testObj = new FedoraSitemap();
        testObj.objectService = mockObjects;
        mockRepo = mock(Repository.class);
        mockSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        testObj.setRepository(mockRepo);
        testObj.setUriInfo(getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetSitemapIndex() throws Exception {
        when(mockObjects.getRepositoryObjectCount(mockSession)).thenReturn(49999L);

        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(1, sitemapIndex.getSitemapEntries().size());
    }

    @Test
    public void testGetSitemapIndexMultiplePages() throws Exception {
        when(mockObjects.getRepositoryObjectCount(mockSession)).thenReturn(50001L);

        final SitemapIndex sitemapIndex = testObj.getSitemapIndex();

        assertEquals(2, sitemapIndex.getSitemapEntries().size());
    }
}
