package org.fcrepo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

public class TxAwareSessionTest {
    private Session mockSession;
    private Session testObj;

    @Before
    public void setUp() {
        mockSession = mock(Session.class);
        testObj = TxAwareSession.newInstance(mockSession, "txid");


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
