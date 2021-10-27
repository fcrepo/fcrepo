/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Represents the case where a property definition has been requested but does
 * not exist. Typically, this happens when a new property is added to a node
 * that does not restrict its property types.
 *
 * @author ajs6f
 * @since Oct 25, 2013
 */
public class NoSuchPropertyDefinitionException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public NoSuchPropertyDefinitionException(final String msg) {
        super(msg);
    }
}
