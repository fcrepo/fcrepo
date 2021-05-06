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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;

import org.mockito.Mockito;

/**
 * TX help for testing
 *
 * @author pwinckles
 */
public class TestTransactionHelper {

    private TestTransactionHelper() {

    }

    /**
     * Create a mock transaction.
     * @param transactionId the id of the transaction
     * @param isShortLived is the transaction short-lived.
     * @return the mock transaction.
     */
    public static Transaction mockTransaction(final String transactionId, final boolean isShortLived) {
        final var transaction = Mockito.mock(Transaction.class);
        when(transaction.getId()).thenReturn(transactionId);
        when(transaction.isShortLived()).thenReturn(isShortLived);
        when(transaction.isOpenLongRunning()).thenReturn(!isShortLived);
        when(transaction.isOpen()).thenReturn(true);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return null;
        }).when(transaction).doInTx(any(Runnable.class));
        return transaction;
    }

}
