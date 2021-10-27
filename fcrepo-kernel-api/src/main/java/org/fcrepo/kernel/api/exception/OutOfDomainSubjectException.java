/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

import org.apache.jena.graph.Node;

/**
 * Fedora does not accept RDF with subjects that are not local to the repository.
 *
 * @author whikloj
 * @since 2015-05-29
 */
public class OutOfDomainSubjectException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Takes the subject that is out of domain, creates message.
     *
     * @param subject the subject
     */
    public OutOfDomainSubjectException(final Node subject) {
        super(String.format("RDF Stream contains subject(s) (%s) not in the domain of this repository.", subject));
    }

}
