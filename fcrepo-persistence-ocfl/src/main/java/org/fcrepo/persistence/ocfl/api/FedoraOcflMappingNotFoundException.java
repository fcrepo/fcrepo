/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.api;


/**
 * Indicates the fedora identifier was not found in the index.
 *
 * @author dbernstein

 */
public class FedoraOcflMappingNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public FedoraOcflMappingNotFoundException(final String msg) {
        super(msg);
    }

    /**
     * Constructor for wrapping exception.
     *
     * @param exception the original exception.
     */
    public FedoraOcflMappingNotFoundException(final Throwable exception) {
        super(exception);
    }

}
