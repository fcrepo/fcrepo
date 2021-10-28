/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.observer;

import org.fcrepo.kernel.api.Transaction;
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
     * @param transaction the transaction
     * @param fedoraId the id of the affected resource
     * @param operation the operation affecting the resource
     */
    void recordEventForOperation(Transaction transaction, FedoraId fedoraId, ResourceOperation operation);

    /**
     * Emits all of the events that were accumulated within the transaction. Multiple events affecting the same resource
     * are combined into a single event.
     *
     * <p>This method SHOULD NOT throw an exception if an event fails to be emitted. It should always attempt to emit
     * all events accumulated within a transaction.
     *
     * @param transaction the transaction
     * @param baseUrl the baseUrl of the requests
     * @param userAgent the user-agent of the user making the requests
     */
    void emitEvents(Transaction transaction, String baseUrl, String userAgent);

    /**
     * Removes all of a transaction's accumulated events without emitting them. This must be called when a transaction
     * is rolled back.
     *
     * @param transaction the id of the transaction
     */
    void clearEvents(Transaction transaction);

}
