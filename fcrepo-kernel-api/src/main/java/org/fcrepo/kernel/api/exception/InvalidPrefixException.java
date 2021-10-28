/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;


/**
 * Indicates a prefix used in a CRUD request has existed in the repository
 *
 * @author nianma
 * @since Nov 2, 2015
 */
public class InvalidPrefixException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public InvalidPrefixException(final String msg) {
        super(msg);
    }

}
