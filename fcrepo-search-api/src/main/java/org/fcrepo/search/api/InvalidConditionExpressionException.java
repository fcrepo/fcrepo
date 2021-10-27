/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

/**
 * An exception that indicates that the syntax of the condition expression
 * is invalid and therefore cannot be parsed.
 *
 * @author dbernstein
 */
public class InvalidConditionExpressionException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public InvalidConditionExpressionException(final String msg) {
        super(msg);
    }

}
