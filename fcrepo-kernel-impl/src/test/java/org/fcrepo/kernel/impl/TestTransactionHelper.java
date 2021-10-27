/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
