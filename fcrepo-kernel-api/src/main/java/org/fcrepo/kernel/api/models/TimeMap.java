/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import java.time.Instant;
import java.util.List;

/**
 * @author lsitu
 * @since Oct. 04, 2017
 */
public interface TimeMap extends FedoraResource {
    /**
     * List all of the memento datetimes for the resource
     * @return list of memento datetimes, in ascending chronologic order
     */
    public List<Instant> listMementoDatetimes();
}
