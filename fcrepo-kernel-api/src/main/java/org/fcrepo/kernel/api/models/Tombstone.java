/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

/**
 * @author cabeer
 * @since 10/16/14
 */
public interface Tombstone extends FedoraResource {

    /**
     * Return the object this tombstone is for.
     * @return the original deleted resource.
     */
    FedoraResource getDeletedObject();

}
