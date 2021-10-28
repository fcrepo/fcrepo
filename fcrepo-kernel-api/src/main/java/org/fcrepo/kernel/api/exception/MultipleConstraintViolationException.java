/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper to hold multiple constraint violation exceptions for later reporting.
 * @author whikloj
 */
public class MultipleConstraintViolationException extends ConstraintViolationException {

    private Set<ConstraintViolationException> exceptionTypes = new HashSet<>();

    private String fullMessage = "";

    public Set<ConstraintViolationException> getExceptionTypes() {
        return exceptionTypes;
    }

    public MultipleConstraintViolationException(final List<ConstraintViolationException> exceptions) {
        super("There are multiple exceptions");
        for (final ConstraintViolationException exception : exceptions) {
            exceptionTypes.add(exception);
            fullMessage += exception.getMessage() + "\n";
        }
    }

    @Override
    public String getMessage() {
        return fullMessage;
    }

}
