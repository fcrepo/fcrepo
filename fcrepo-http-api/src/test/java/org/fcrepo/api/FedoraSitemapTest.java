package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraSitemapTest {

    FedoraSitemap testObj;

    ObjectService mockObjects;


    Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(ObjectService.class);
        testObj = new FedoraSitemap();
        testObj.objectService = mockObjects;
        mockSession = mock(Session.class);
    	SessionFactory mockSessions = mock(SessionFactory.class);
    	when(mockSessions.getSession()).thenReturn(mockSession);
    	when(mockSessions.getSession(any(SecurityContext.class), any(HttpServletRequest.class))).thenReturn(mockSession);
        testObj.setSessionFactory(mockSessions);
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
