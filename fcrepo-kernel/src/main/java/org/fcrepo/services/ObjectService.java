
package org.fcrepo.services;

import static com.google.common.collect.ImmutableSet.builder;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
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

    public Node getObjectNode(final Session session, final String pid)
            throws RepositoryException {
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
    public FedoraObject getObject(final Session session, final String pid)
            throws RepositoryException {
        logger.trace("Executing getObject() with pid: " + pid);
        return new FedoraObject(getObjectNode(session, pid));
    }
    
    public FedoraObject getObjectByPath(final Session session, final String path)
            throws RepositoryException {
        return new FedoraObject(session, path);
    }
    
    public FedoraObject getObjectByPath(String path)
            throws RepositoryException {
        return new FedoraObject(readOnlySession, path);
    }

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    public Set<String> getObjectNames(String path) throws RepositoryException {

        final Node objects = readOnlySession.getNode(getObjectJcrNodePath(path));
        final Builder<String> b = builder();
        final NodeIterator i = objects.getNodes();
        while (i.hasNext()) {
            Node n = i.nextNode();
            logger.info("child of type {} is named {} at {}", n.getPrimaryNodeType(), n.getName(), n.getPath());
            if (n.isNodeType("nt:folder")) b.add(n.getName());
        }
        return b.build();

    }

    public void deleteObject(final String pid, final Session session)
            throws RepositoryException {
        final Node obj = session.getNode(getObjectJcrNodePath(pid));
        obj.remove();
        session.save();
    }

    public void deleteObjectByPath(final String path, final Session session)
            throws RepositoryException {
        final Node obj = session.getNode(path);
        obj.remove();
        session.save();
    }
}
