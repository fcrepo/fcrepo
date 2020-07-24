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

package org.fcrepo.kernel.api.observer;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperation;

/**
 * Accumulates events for changes made to resources, grouped by transaction. The events are not emitted until after the
 * transaction has been committed. If a transaction is rolled back, {@link #clearEvents} MUST be called to release the
 * stored events.
 *
 * @author pwinckles
 */
public interface EventAccumulator {

    /**
     * Registers a new event to a transaction.
     *
     * @param transactionId the id of the transaction
     * @param fedoraId the id of the affected resource
     * @param operation the operation affecting the resource
     */
    void recordEventForOperation(String transactionId, FedoraId fedoraId, ResourceOperation operation);

    /**
     * Emits all of the events that were accumulated within the transaction. Multiple events affecting the same resource
     * are combined into a single event.
     *
     * <p>This method SHOULD NOT throw an exception if an event fails to be emitted. It should always attempt to emit
     * all events accumulated within a transaction.
     *
     * @param transactionId the id of the transaction
     * @param baseUrl the baseUrl of the requests
     * @param userAgent the user-agent of the user making the requests
     */
    void emitEvents(String transactionId, String baseUrl, String userAgent);

    /**
     * Removes all of a transaction's accumulated events without emitting them. This must be called when a transaction
     * is rolled back.
     *
     * @param transactionId the id of the transaction
     */
    void clearEvents(String transactionId);

}
