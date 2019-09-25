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
package org.fcrepo.kernel.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.fcrepo.kernel.api.FedoraTransactionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>TransactionTest class.</p>
 *
 * @author mohideen
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraTransactionManagerTest {

    private FedoraTransactionImpl testTx;

    private FedoraTransactionManager testTxManager;

    @Before
    public void setUp() {
        testTxManager = new FedoraTransactionManagerImpl();
        testTx = (FedoraTransactionImpl) testTxManager.create();
    }

    @Test
    public void testCreateTransaction() {
        testTx = (FedoraTransactionImpl) testTxManager.create();
        assertNotNull(testTx);
    }

    @Test
    public void testGetTransaction() {
        final FedoraTransactionImpl tx = (FedoraTransactionImpl) testTxManager.get(testTx.getId());
        assertNotNull(tx);
        assertEquals(testTx.getId(), tx.getId());
    }

    @Test(expected = RuntimeException.class)
    public void testGetTransactionWithInvalidID() {
        final FedoraTransactionImpl tx = (FedoraTransactionImpl) testTxManager.get("invalid-id");
    }
}
