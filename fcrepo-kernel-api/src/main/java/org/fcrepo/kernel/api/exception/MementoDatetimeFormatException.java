/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Indicates that the wrong format of the Memento-Datetime.
 *
 * @author lsitu
 * @since 2018-03-12
 */
public class MementoDatetimeFormatException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public MementoDatetimeFormatException(final String msg, final Throwable rootCause) {
        super(msg,rootCause);
    }

}
