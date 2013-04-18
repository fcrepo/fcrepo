
package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getQueryMock;
import static org.fcrepo.api.TestHelpers.getQuerySessionMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.session.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraFieldSearchTest {

    FedoraFieldSearch testObj;

    Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockSession = getQuerySessionMock();
        final SessionFactory mockSessions = mock(SessionFactory.class);
        when(mockSessions.getSession()).thenReturn(mockSession);
        when(
                mockSessions.getSession(any(SecurityContext.class),
                        any(HttpServletRequest.class))).thenReturn(mockSession);
        testObj = new FedoraFieldSearch();
        testObj.setSessionFactory(mockSessions);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testSearchSubmit() throws RepositoryException {
        final String actual = testObj.searchSubmit("foo", "1", "1");
        assertTrue("actual.length = " + actual.length() + "; expected > 0",
                actual.length() > 0);
    }

    @Test
    public void testGetQuery() throws RepositoryException {
        final QueryManager queryManager = mock(QueryManager.class);
        final ValueFactory valueFactory = mock(ValueFactory.class);
        final Value mockValue = mock(Value.class);
        final String terms = "foo";
        final Query mockQuery = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2)))
                .thenReturn(mockQuery);
        when(valueFactory.createValue("%" + terms + "%")).thenReturn(mockValue);
        final Query actual =
                testObj.getQuery(queryManager, valueFactory, terms);
        assertNotNull(actual);
        verify(queryManager).createQuery(anyString(), eq(Query.JCR_SQL2));
        verify(valueFactory).createValue("%" + terms + "%");
        verify(mockQuery).bindValue("sterm", mockValue);
    }

    @Test
    public void testSearch() throws RepositoryException {
        final Query mockQ = getQueryMock();
        final NodeIterator mockNodes = mockQ.execute().getNodes();
        final FieldSearchResult actual = testObj.search(mockQ, 1, 1);
        final List<ObjectFields> oFieldsList = actual.getObjectFieldsList();
        assertEquals(1, oFieldsList.size());
        final ObjectFields oFields = oFieldsList.get(0);
        // because the mock nodeIterator doesn't respond to skip
        assertEquals("node1", oFields.getPid());
        // the first time, unfortunately, is at the beginning of this test to get the NodeIterator mock
        verify(mockQ, times(2)).execute();
        verify(mockNodes).skip(1);
    }
}
