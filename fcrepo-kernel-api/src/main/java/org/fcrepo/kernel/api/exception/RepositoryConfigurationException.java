/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;


/**
 * Indicates an error in the configuration of the repository
 *
 * @since 2015-10-31
 * @author awoods
 */
public class RepositoryConfigurationException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public RepositoryConfigurationException(final String msg) {
        super(msg);
    }

}
