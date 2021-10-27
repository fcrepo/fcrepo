/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Request failed with ACL link header
 *
 * @author lsitu
 * @since 2018-07-25
 */
public class RequestWithAclLinkHeaderException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public RequestWithAclLinkHeaderException(final String msg) {
        super(msg);
    }

}
