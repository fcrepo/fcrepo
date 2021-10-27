/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Represents the condition that an attempt has been made to persist RDF to the repository as properties of a
 * particular resource, but the subject of a triple in that RDF is not the resource in question.
 *
 * @author ajs6f
 */
public class IncorrectTripleSubjectException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message the error message
     */
    public IncorrectTripleSubjectException(final String message) {
        super(message);
    }

}
