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
