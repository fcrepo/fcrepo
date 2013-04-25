
package org.fcrepo.services;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Convenience class for constructing Fedora-related paths in the JCR repository.
 * 
 * @author cbeer
 *
 */
public abstract class PathService {

    public static final String OBJECT_PATH = "/objects";
    
    public static final String FEDERATED_PATH = "/federated";

    private static final Logger logger = getLogger(PathService.class);

    /**
     * @param pid
     * @return The JCR path to this object's backing node.
     */
    public static String getObjectJcrNodePath(final String pid) {
        logger.trace("Executing getObjectJcrNodePath() with pid: " + pid);
        if (pid.startsWith("/")) return pid; // already absolute
        return OBJECT_PATH + "/" + pid;
    }

    /**
     * @param pid
     * @param dsId
     * @return The JCR path to this datastream's backing node.
     */
    public static String getDatastreamJcrNodePath(final String pid,
            final String dsId) {
        logger.trace("Executing getDatastreamJcrNodePath() with pid: " + pid +
                " and dsId: " + dsId);
        if (pid.startsWith("/")) {
            // absolute path
            return pid + "/" + dsId;
        }
        return getObjectJcrNodePath(pid) + "/" + dsId;
    }
    
    /**
     * @param pid
     * @return The federated JCR path to this object's backing node.
     */
    public static String getFederatedNodePath(String pid) {
    	logger.trace("Executing getFederatedNodePath() with pid: " + pid);
        return FEDERATED_PATH + "/" + pid;
    }
    
    /**
     * @param pid
     * @param dsId
     * @return The federated JCR path to this datastream's backing node.
     */
    public static String getFederatedDsNodePath(String pid, String dsId) {
        logger.trace("Executing getFederatedDsNodePath() with pid: " + pid +
                " and dsId: " + dsId);
        return getFederatedNodePath(pid) + "/" + dsId;
    }
}
