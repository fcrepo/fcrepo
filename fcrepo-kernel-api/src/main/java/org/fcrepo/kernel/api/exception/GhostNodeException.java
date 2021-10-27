/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception when trying to alter an immutable resource.
 * @author whikloj
 */
public class GhostNodeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    public GhostNodeException(final String msg) {
        super(msg);
    }
}
