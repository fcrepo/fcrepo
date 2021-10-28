/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception thrown if a Relaxable SMT is altered.
 * @author whikloj
 * @since 6.0.0
 */
public class RelaxableServerManagedPropertyException extends ServerManagedPropertyException {

    /**
     *
     * @param msg the message
     */
    public RelaxableServerManagedPropertyException(final String msg) {
        super(msg);
    }
}
