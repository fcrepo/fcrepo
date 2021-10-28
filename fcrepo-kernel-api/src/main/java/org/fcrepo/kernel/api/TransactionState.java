/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api;

/**
 * Represents the state of a transaction
 *
 * @author pwinckles
 */
public enum TransactionState {

    OPEN,
    COMMITTING,
    COMMITTED,
    FAILED,
    ROLLINGBACK,
    ROLLEDBACK,

}
