/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

/**
 * An exception that the set of parameters  defining the search  query
 * is invalid and therefore cannot be processed.
 *
 * @author dbernstein
 */
public class InvalidQueryException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public InvalidQueryException(final String msg) {
        super(msg);
    }

}
