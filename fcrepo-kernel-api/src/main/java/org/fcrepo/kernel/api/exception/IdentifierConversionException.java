/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * @author cabeer
 * @since 10/7/14
 */
public class IdentifierConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public IdentifierConversionException(final String msg) {
        super(msg);
    }

}
