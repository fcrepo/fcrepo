
package org.fcrepo.services;

import static com.google.common.collect.ImmutableSet.builder;
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
     * @param path The path to use to create the object
     * @return The created object
     * @throws RepositoryException
     */
    public FedoraObject createObject(final Session session, final String path)
            throws RepositoryException {
        return new FedoraObject(session, path);
    }

	/**
	 * @param path
	 * @return The JCR node behind the FedoraObject with the proffered PID
	 * @throws RepositoryException
	 */
    public Node getObjectNode(final Session session, final String path)
            throws RepositoryException {
        return session.getNode(path);
    }

    /**
     * @param path
     * @param session
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public FedoraObject getObject(final Session session, final String path)
            throws RepositoryException {
        logger.trace("Executing getObject() with path: {}", path);
        return new FedoraObject(getObjectNode(session, path));
    }

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    public Set<String> getObjectNames(final Session session, String path) throws RepositoryException {
        return getObjectNames(session, path, null);
    }
    
    public Set<String> getObjectNames(final Session session, String path, String mixin) throws RepositoryException {

        final Node objects = session.getNode(path);
        final Builder<String> b = builder();
        final NodeIterator i = objects.getNodes();
        while (i.hasNext()) {
            Node n = i.nextNode();
            logger.info("child of type {} is named {} at {}", n.getPrimaryNodeType(), n.getName(), n.getPath());
            if (mixin == null || n.isNodeType(mixin)) b.add(n.getName());
        }
        return b.build();

    }

    public void deleteObject(final Session session, final String path)
            throws RepositoryException {
        final Node obj = session.getNode(path);
        obj.remove();
        session.save();
    }

}
