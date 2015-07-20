/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.api.Transaction.State.COMMITED;
import static org.fcrepo.kernel.api.Transaction.State.DIRTY;
import static org.fcrepo.kernel.api.Transaction.State.NEW;
import static org.fcrepo.kernel.api.Transaction.State.ROLLED_BACK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>TransactionImplTest class.</p>
 *
 * @author ksclarke
 */
public class TransactionImplTest {

    private Transaction testObj;

    private static final String USER_NAME = "test";

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new TransactionImpl(mockSession, USER_NAME);
    }

    @Test
    public void testRollback() throws RepositoryException {
        testObj.rollback();
        verify(mockSession).refresh(false);
        verify(mockSession).logout();
        assertEquals(ROLLED_BACK, testObj.getState());
        final long update = testObj.getExpires().getTime();
        assertTrue(update <= currentTimeMillis());
    }

    @Test
    public void testCommit() throws RepositoryException {
        testObj.commit();
        verify(mockSession).save();
        verify(mockSession).logout();
        assertEquals(COMMITED, testObj.getState());
        final long update = testObj.getExpires().getTime();
        assertTrue(update <= currentTimeMillis());
    }

    @Test
    public void testExpire() throws RepositoryException {
        final long orig = testObj.getExpires().getTime();
        testObj.expire();
        verify(mockSession, never()).save();
        verify(mockSession).logout();
        final long update = testObj.getExpires().getTime();
        assertTrue(update < orig);
        assertTrue(update <= currentTimeMillis());
        assertTrue(update < orig);
    }

    @Test
    public void testExpiryUpdate() {
        final long orig = testObj.getExpires().getTime();
        testObj.updateExpiryDate();
        final long update = testObj.getExpires().getTime();
        assertTrue("Unexpected negative expiry delta: " + (update - orig),
                update - orig >= 0);
    }

    @Test
    public void testState() throws RepositoryException {
        assertEquals(NEW, testObj.getState());
        when(mockSession.hasPendingChanges()).thenReturn(true, false);
        assertEquals(DIRTY, testObj.getState());
        testObj.commit();
    }

    @Test
    public void testUserAssociation() {
        final String otherUser = "dummy";
        assertTrue("Transaction expected to be associated with user " + USER_NAME,
                testObj.isAssociatedWithUser(USER_NAME));
        assertFalse("Transaction should not be associated with the user" + otherUser,
                testObj.isAssociatedWithUser(otherUser));
        assertFalse("Transaction should not be associated with an empty user",
                testObj.isAssociatedWithUser(null));

        testObj = new TransactionImpl(mockSession, null);
        assertTrue("Transaction should not be associated with a user",
                testObj.isAssociatedWithUser(null));
        assertFalse("Transaction should not be associated with a user",
                testObj.isAssociatedWithUser(USER_NAME));
    }
}
