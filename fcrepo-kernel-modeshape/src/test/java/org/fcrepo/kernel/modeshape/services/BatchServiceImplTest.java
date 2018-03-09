/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services;

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.FCREPO_TX_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.fcrepo.kernel.api.services.BatchService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author frank asseg
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class BatchServiceImplTest {

    private static final String IS_A_TX = "foo";

    private static final String NOT_A_TX = "bar";

    private static final String USER_NAME = "test";

    private static final String ANOTHER_USER_NAME = "another";

    BatchService service;

    @Mock
    private FedoraSession mockTx;

    @Mock
    private Session mockSession;

    private FedoraSession fedoraSession;

    @Before
    public void setup() throws Exception {
        fedoraSession = new FedoraSessionImpl(mockSession);
        service = new BatchServiceImpl();
        when(mockTx.getId()).thenReturn(IS_A_TX);
        when(mockTx.getUserURI()).thenReturn(null);
        final Field txsField =
                BatchServiceImpl.class.getDeclaredField("sessions");
        txsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, FedoraSession> txs =
                (Map<String, FedoraSession>) txsField
                        .get(BatchService.class);
        txs.put(":" + IS_A_TX, mockTx);
    }

    @Test
    public void testExpiration() {
        final Instant fiveSecondsAgo = now().minusSeconds(5);
        when(mockTx.getExpires()).thenReturn(of(fiveSecondsAgo));
        service.removeExpired();
        verify(mockTx).expire();
    }

    @Test
    public void testExpirationThrowsRepositoryException() {
        final Instant fiveSecondsAgo = now().minusSeconds(5);
        doThrow(new RepositoryRuntimeException("")).when(mockTx).expire();
        when(mockTx.getExpires()).thenReturn(of(fiveSecondsAgo));
        service.removeExpired();
    }

    @Test
    public void testCreateTx() {
        service.begin(fedoraSession);
        assertTrue(service.exists(fedoraSession.getId()));
        assertTrue(service.exists(fedoraSession.getId(), null));
        assertEquals(service.getSession(fedoraSession.getId()).getId(), fedoraSession.getId());
    }

    @Test
    public void testGetTx() {
        final FedoraSession tx = service.getSession(IS_A_TX, null);
        assertNotNull(tx);
    }

    @Test(expected = SessionMissingException.class)
    public void testHijackingNotPossible() {
        service.begin(fedoraSession);
        service.getSession(fedoraSession.getId(), ANOTHER_USER_NAME);
    }

    @Test(expected = SessionMissingException.class)
    public void testHijackingNotPossibleWithAnonUser() {
        service.begin(fedoraSession, USER_NAME);
        service.getSession(fedoraSession.getId(), null);
    }

    @Test(expected = SessionMissingException.class)
    public void testHijackingNotPossibleWhenStartedAnonUser() {
        when(mockSession.getUserID()).thenReturn(USER_NAME);
        service.begin(fedoraSession);
        service.getSession(fedoraSession.getId(), ANOTHER_USER_NAME);
    }

    @Test(expected = SessionMissingException.class)
    public void testGetNonTx() throws SessionMissingException {
        service.getSession(NOT_A_TX, null);
    }

    @Test
    public void testGetTxForSession() throws Exception {
        when(mockSession.getNamespaceURI(FCREPO_TX_ID)).thenReturn(IS_A_TX);
        when(mockTx.getId()).thenReturn(IS_A_TX);
        final FedoraSession tx = service.getSession(mockTx.getId());
        assertEquals(IS_A_TX, tx.getId());
    }

    @Test(expected = SessionMissingException.class)
    public void testGetTxForNonTxSession() throws RepositoryException {
        when(mockSession.getNamespaceURI(FCREPO_TX_ID)).thenThrow(new NamespaceException(""));
        service.getSession(fedoraSession.getId());
    }

    @Test
    public void testExists() {
        assertTrue(service.exists(IS_A_TX));
        assertFalse(service.exists(NOT_A_TX));
    }

    @Test
    public void testCommitTx() {
        service.commit(IS_A_TX);
        verify(mockTx).commit();
    }

    @Test(expected = SessionMissingException.class)
    public void testCommitRemovedSession() {
        service.commit(IS_A_TX);
        service.getSession(fedoraSession.getId(), null);
    }

    @Test
    public void testAbortTx() {
        service.abort(IS_A_TX);
        verify(mockTx).expire();
    }

    @Test(expected = SessionMissingException.class)
    public void testAbortRemovedSession() {
        service.abort(IS_A_TX);
        service.getSession(IS_A_TX, null);
    }

    @Test(expected = SessionMissingException.class)
    public void testAbortWithNonTx() {
        service.abort(NOT_A_TX);
    }

    @Test(expected = SessionMissingException.class)
    public void testCommitWithNonTx() {
        service.commit(NOT_A_TX);
    }
}
