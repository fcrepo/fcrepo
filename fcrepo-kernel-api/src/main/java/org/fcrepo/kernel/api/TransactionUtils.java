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

package org.fcrepo.kernel.api;

/**
 * A utility class for working with common transaction related operations.
 * @author dbernstein
 */
public class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * Returns the transaction  ID if the transaction is both non-null and uncommitted. Otherwise it returns null.
     * @param transaction The transaction
     * @return The transaction ID or null
     */
    public static String openTxId(final Transaction transaction) {
        return transaction == null || transaction.isCommitted() ? null : transaction.getId();
    }
}
