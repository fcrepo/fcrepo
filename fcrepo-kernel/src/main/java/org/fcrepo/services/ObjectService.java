
package org.fcrepo.services;

import static com.google.common.collect.ImmutableSet.builder;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet.Builder;

/**
 * Service for creating and retrieving FedoraObjects without using the JCR API.
 * 
 * @author cbeer
 *
 */
public class ObjectService extends RepositoryService implements FedoraJcrTypes {

    private static final Logger logger = getLogger(ObjectService.class);

    /**
     * @param session A JCR Session
     * @param path JCR path under which to create this object
     * @return
     * @throws RepositoryException
     */
    @Deprecated
    public Node
            createObjectNodeByPath(final Session session, final String path)
                    throws RepositoryException {
        return new FedoraObject(session, path).getNode();
    }

    /**
     * @param session A JCR Session
     * @param name The name (pid) to use to create the object
     * @return the JCR node behind the created object
     * @throws RepositoryException
     */
    public Node createObjectNode(final Session session, final String name)
            throws RepositoryException {
        return new FedoraObject(session, getObjectJcrNodePath(name)).getNode();
    }

    /**
     * @param session A JCR Session
     * @param name The name (pid) to use to create the object
     * @return The created object
     * @throws RepositoryException
     */
    public FedoraObject createObject(final Session session, final String name)
            throws RepositoryException {
        return new FedoraObject(session, getObjectJcrNodePath(name));
    }

    /**
     * @param pid
     * @return The JCR node behind the FedoraObject with the proferred PID
     * @throws RepositoryException
     */
    public Node getObjectNode(final String pid) throws RepositoryException {
        logger.trace("Executing getObjectNode() with pid: " + pid);
        return getObjectNode(readOnlySession, pid);
    }
    
    public Node getObjectNode(final Session session, final String pid) throws RepositoryException {
        return session.getNode(getObjectJcrNodePath(pid));
    }

    /**
     * @param pid
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public FedoraObject getObject(final String pid) throws RepositoryException {
        logger.trace("Executing getObject() with pid: " + pid);
        return new FedoraObject(getObjectNode(pid));
    }
    
    /**
     * @param pid
     * @param session
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public FedoraObject getObject(Session session, String pid) throws RepositoryException {
        logger.trace("Executing getObject() with pid: " + pid);
    	return new FedoraObject(getObjectNode(session, pid));
    }

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    public Set<String> getObjectNames() throws RepositoryException {

        Node objects = readOnlySession.getNode(getObjectJcrNodePath(""));
        Builder<String> b = builder();
        for (final NodeIterator i = objects.getNodes(); i.hasNext();) {
            b.add(i.nextNode().getName());
        }
        return b.build();

    }

    public void deleteObject(String pid, Session session) throws PathNotFoundException, RepositoryException {
    	Node obj =session.getNode(getObjectJcrNodePath(pid));
    	long objSize = getObjectSize(obj);
        obj.remove();
        session.save();
        updateRepositorySize(0L - objSize, session);
        session.save();
    }

}
