/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception when trying to use an unsupported media type.
 * @author whikloj
 * @since 6.0.0
 */
public class UnsupportedMediaTypeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     * @param msg the exception message.
     */
    public UnsupportedMediaTypeException(final String msg) {
        super(msg);
    }
}
