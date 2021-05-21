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

package org.fcrepo.common.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * Wrapper around Spring's db transaction management
 *
 * @author pwinckles
 */
@Component
public class DbTransactionExecutor {

    private static final Logger LOGGER = getLogger(DbTransactionExecutor.class);

    private static final RetryPolicy<Object> DB_RETRY = new RetryPolicy<>()
            .handleIf(e -> {
                return e instanceof DeadlockLoserDataAccessException
                        || (e.getCause() != null && e.getCause() instanceof DeadlockLoserDataAccessException);
            })
            .onRetry(event -> {
                LOGGER.debug("Retrying operation that failed with the following exception", event.getLastFailure());
            })
            .withBackoff(10, 100, ChronoUnit.MILLIS, 1.5)
            .withJitter(0.1)
            .withMaxRetries(10);

    @Autowired
    private TransactionTemplate transactionTemplate;

    public DbTransactionExecutor() {

    }

    public DbTransactionExecutor(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Executes the runnable within a DB transaction that will retry entire block on MySQL deadlock exceptions.
     *
     * @param action the code to execute
     */
    public void doInTxWithRetry(final Runnable action) {
        Failsafe.with(DB_RETRY).run(() -> {
            doInTx(action);
        });
    }

    /**
     * Executes the runnable within a DB transaction. MySQL deadlock exceptions are NOT retried.
     *
     * @param action the code to execute
     */
    public void doInTx(final Runnable action) {
        if (transactionTemplate == null) {
            // If the transaction template is not set, just execute the code without a tx.
            // This will never happen when configured by Spring, but is useful when unit testing
            LOGGER.warn("Executing outside of a DB transaction");
            action.run();
        } else {
            transactionTemplate.executeWithoutResult(status -> {
                action.run();
            });
        }
    }

}
