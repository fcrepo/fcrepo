
package org.fcrepo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for constructing Fedora-related paths in the JCR repository.
 * 
 * @author cbeer
 *
 */
public class PathService {

    public final static String OBJECT_PATH = "/objects";

    private static final Logger logger = LoggerFactory
            .getLogger(DatastreamService.class);

    /**
     * @param pid
     * @return The JCR path to this object's backing node.
     */
    public static String getObjectJcrNodePath(String pid) {
        logger.trace("Executing getObjectJcrNodePath() with pid: " + pid);
        return OBJECT_PATH + "/" + pid;
    }

    /**
     * @param pid
     * @param dsId
     * @return The JCR path to this datastream's backing node.
     */
    public static String getDatastreamJcrNodePath(String pid, String dsId) {
        logger.trace("Executing getDatastreamJcrNodePath() with pid: " + pid +
                " and dsId: " + dsId);
        return getObjectJcrNodePath(pid) + "/" + dsId;
    }
}
