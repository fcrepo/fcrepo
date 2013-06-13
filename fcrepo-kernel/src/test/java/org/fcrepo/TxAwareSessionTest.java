package org.fcrepo;

import org.junit.Before;
import org.junit.Test;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TxAwareSessionTest {
    private Session mockSession;
    private Session testObj;

    @Before
    public void setUp() {
        mockSession = mock(Session.class);
        testObj = TxAwareSession.newInstance(mockSession);


    }

    @Test
    public void shouldProxyMethods() throws RepositoryException {
        testObj.getItem("/xyz");
        verify(mockSession).getItem("/xyz");
    }

    @Test
    public void shouldMakeLogoutANoop() {
        testObj.logout();
        verify(mockSession, never()).logout();
    }

    @Test
    public void shouldMakeSaveANoop() throws RepositoryException {
        testObj.save();
        verify(mockSession, never()).save();
    }

}
