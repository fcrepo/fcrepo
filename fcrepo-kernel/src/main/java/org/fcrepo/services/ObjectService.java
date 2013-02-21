
package org.fcrepo.services;

import static com.google.common.collect.ImmutableSet.builder;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet.Builder;

public class ObjectService {

    private static final Logger logger = LoggerFactory
            .getLogger(ObjectService.class);

    @Inject
    private Repository repo;

    /**
     * For use with non-mutating methods.
     */
    private static Session readOnlySession;

    /**
     * @param session A JCR Session
     * @param path JCR path under which to create this object
     * @return
     * @throws RepositoryException
     */
    @Deprecated
    public static Node createObjectNodeByPath(final Session session,
            final String path) throws RepositoryException {
        return new FedoraObject(session, path).getNode();
    }

    /**
     * @param session A JCR Session
     * @param name The name (pid) to use to create the object
     * @return the JCR node behind the created object
     * @throws RepositoryException
     */
    public static Node
            createObjectNode(final Session session, final String name)
                    throws RepositoryException {
        return new FedoraObject(session, getObjectJcrNodePath(name)).getNode();
    }

    /**
     * @param session A JCR Session
     * @param name The name (pid) to use to create the object
     * @return The created object
     * @throws RepositoryException
     */
    public static FedoraObject createObject(final Session session,
            final String name) throws RepositoryException {
        return new FedoraObject(session, getObjectJcrNodePath(name));
    }

    /**
     * @param pid
     * @return The JCR node behind the FedoraObject with the proferred PID
     * @throws RepositoryException
     */
    public static Node getObjectNode(final String pid)
            throws RepositoryException {
        logger.trace("Executing getObjectNode() with pid: " + pid);
        return getObject(pid).getNode();
    }

    /**
     * @param pid
     * @return A FedoraObject with the proferred PID
     * @throws RepositoryException
     */
    public static FedoraObject getObject(final String pid)
            throws RepositoryException {
        logger.trace("Executing getObject() with pid: " + pid);
        return new FedoraObject(readOnlySession
                .getNode(getObjectJcrNodePath(pid)));
    }

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    public static Set<String> getObjectNames() throws RepositoryException {

        Node objects = readOnlySession.getNode(getObjectJcrNodePath(""));
        Builder<String> b = builder();
        for (NodeIterator i = objects.getNodes(); i.hasNext();) {
            b.add(i.nextNode().getName());
        }
        return b.build();

    }

    @PostConstruct
    public void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public void logoutSession() {
        readOnlySession.logout();
    }

}
