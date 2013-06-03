package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

public class TransactionTest {

    private Transaction testObj;

    private Session mockSession;

    @Before
    public void setUp(){
        mockSession = mock(Session.class);
        testObj = new Transaction(mockSession);
    }

    @Test
    public void testRollback() throws RepositoryException {
        testObj.rollback();
        verify(mockSession).refresh(false);
        verify(mockSession).logout();
        assertEquals(Transaction.State.ROLLED_BACK, testObj.getState());
        long update = testObj.getExpires().getTime();
        assertTrue(update <= System.currentTimeMillis());
    }

    @Test
    public void testCommit() throws RepositoryException {
        testObj.commit();
        verify(mockSession).save();
        verify(mockSession).logout();
        assertEquals(Transaction.State.COMMITED, testObj.getState());
        long update = testObj.getExpires().getTime();
        assertTrue(update <= System.currentTimeMillis());
    }

    @Test
    public void testExpire() throws RepositoryException {
        long orig = testObj.getExpires().getTime();
        testObj.expire();
        verify(mockSession, never()).save();
        verify(mockSession).logout();
        long update = testObj.getExpires().getTime();
        assertTrue(update < orig);
        assertTrue(update <= System.currentTimeMillis());
        assertTrue(update < orig);
    }

    @Test
    public void testExpiryUpdate() throws RepositoryException {
        long orig = testObj.getExpires().getTime();
        testObj.updateExpiryDate();
        long update = testObj.getExpires().getTime();
        assertTrue("Unexpected negative expiry delta: " + (update - orig),
                update - orig >= 0);
    }

    @Test
    public void testState() throws RepositoryException {
        assertEquals(Transaction.State.NEW, testObj.getState());
        when(mockSession.hasPendingChanges()).thenReturn(true, false);
        assertEquals(Transaction.State.DIRTY, testObj.getState());
        testObj.commit();
    }
}
