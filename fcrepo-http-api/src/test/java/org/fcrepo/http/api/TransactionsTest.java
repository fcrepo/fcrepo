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
package org.fcrepo.http.api;

import static java.time.Instant.now;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.security.Principal;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>TransactionsTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionsTest {

    private Transactions testObj;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private TransactionManager mockTxManager;

    @Mock
    private HttpResourceConverter mockTranslator;

    @Mock
    private Principal mockPrincipal;

    @Mock
    private SecurityContext mockSecurityContext;

    @Before
    public void setUp() {
        testObj = new Transactions();
        mockTransaction.setShortLived(false);
        when(mockTxManager.create()).thenReturn(mockTransaction);
        when(mockTxManager.get("tx:123")).thenReturn(mockTransaction);
        when(mockTxManager.get(AdditionalMatchers.not(ArgumentMatchers.eq("tx:123"))))
            .thenThrow(new RuntimeException("No Transaction found with transactionId"));
        when(mockTransaction.getId()).thenReturn("123");
        when(mockTransaction.getExpires()).thenReturn(now().plusSeconds(100));
        setField(testObj, "txManager", mockTxManager);
    }

    @Ignore // TODO Enable after HttpResourceConvertor.toDomain is implemented
    @Test
    public void shouldStartANewTransaction() throws URISyntaxException {
        setField(testObj, "transaction", mockTransaction);
        setField(testObj, "idTranslator", mockTranslator);
        final Response response = testObj.createTransaction();
        verify(mockTxManager).create();
        assertEquals(201, response.getStatus());
        assertEquals("/tx:123", response.getHeaderString("Location"));
    }

    @Test
    public void shouldCommitATransaction() {
        final Response response = testObj.commit("tx:123");
        assertEquals(204, response.getStatus());
        verify(mockTransaction).commit();
    }

    @Test
    public void shouldErrorIfCommitNonExistingTransactionId() {
        final Response response = testObj.commit("tx:404");
        assertEquals(400, response.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() {
        final Response response = testObj.rollback("tx:123");
        assertEquals(204, response.getStatus());
        verify(mockTransaction).rollback();
    }

    @Test
    public void shouldErrorIfRollbackNonExistingTransactionId() {
        final Response rollback = testObj.rollback("tx:404");
        assertEquals(400, rollback.getStatus());
    }

}
