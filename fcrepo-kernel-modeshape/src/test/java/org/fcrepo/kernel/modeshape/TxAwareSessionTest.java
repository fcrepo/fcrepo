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

import static org.fcrepo.kernel.modeshape.TxAwareSession.newInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Credentials;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.TxSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>TxAwareSessionTest class.</p>
 *
 * @author awoods
 */
public class TxAwareSessionTest {

    @Mock
    private Session mockSession;

    private Session testObj;

    private static final String PATH = "/xyz";

    @Before
    public void setUp() {
        initMocks(this);
        testObj = newInstance(mockSession, "txid");
    }

    @Test
    public void shouldProxyMethods() throws RepositoryException {
        testObj.getItem(PATH);
        verify(mockSession).getItem(PATH);
    }

    @Test
    public void shouldWrapImpersonateToReturnAnotherTxAwareSession()
            throws RepositoryException {
        final Credentials mockCredentials = mock(Credentials.class);
        when(mockSession.impersonate(mockCredentials)).thenReturn(mockSession);
        final Session s = testObj.impersonate(mockCredentials);

        assertTrue(s instanceof TxSession);

        assertEquals(((TxSession) testObj).getTxId(), ((TxSession) s).getTxId());
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

    @Test(expected = PathNotFoundException.class)
    public void shouldThrowUnderlyingException() throws RepositoryException {
        when(mockSession.getNode(PATH)).thenThrow(
                new PathNotFoundException());
        testObj.getNode(PATH);
    }

}
