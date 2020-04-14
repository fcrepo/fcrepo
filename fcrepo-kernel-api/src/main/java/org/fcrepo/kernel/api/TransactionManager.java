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
 * The Fedora Transaction Manager abstraction
 *
 * @author mohideen
 */
public interface TransactionManager {

    /**
     * Create a new fedora transaction
     *
     * @return {@link Transaction} The new fedora transaction
     */
    Transaction create();

    /**
     * Get an existing fedora transaction
     *
     * @param transactionId the id of the transaction to be returned
     * @return {@link Transaction} the fedora transaction associated with the provided id
     */
    Transaction get(String transactionId);
}
