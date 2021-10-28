/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * An extension of {@link RepositoryRuntimeException} that may be thrown when attempting a
 * operation (or instantiation) of a {@link org.fcrepo.kernel.api.models.FedoraResource}
 * on a different (and incompatible) type.
 *
 * @author Mike Durbin
 */
public class ResourceTypeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     * @param message the message
     */
    public ResourceTypeException(final String message) {
        super(message);
    }

}
