
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
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.fcrepo.FedoraObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet.Builder;

/**
 * Service for creating and retrieving FedoraObjects without using the JCR API.
 * 
 * @author cbeer
 *
 */
public class ObjectService {

    private static final Logger logger = LoggerFactory
            .getLogger(ObjectService.class);

    @Inject
    private Repository repo;

    /**
     * For use with non-mutating methods.
     */
    private Session readOnlySession;

    /**
     * @param session A JCR Session
     * @param path JCR path under which to create this object
     * @return
     * @throws RepositoryException
     */
    @Deprecated
    public Node createObjectNodeByPath(final Session session,
            final String path) throws RepositoryException {
        return new FedoraObject(session, path).getNode();
    }

    /**
     * @param session A JCR Session
     * @param name The name (pid) to use to create the object
     * @return the JCR node behind the created object
     * @throws RepositoryException
     */
    public Node
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
    public FedoraObject createObject(final Session session,
            final String name) throws RepositoryException {
        return new FedoraObject(session, getObjectJcrNodePath(name));
    }

    /**
     * @param pid
     * @return The JCR node behind the FedoraObject with the proferred PID
     * @throws RepositoryException
     */
    public Node getObjectNode(final String pid)
            throws RepositoryException {
        logger.trace("Executing getObjectNode() with pid: " + pid);
        return getObject(pid).getNode();
    }

    /**
     * @param pid
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    public FedoraObject getObject(final String pid)
            throws RepositoryException {
        logger.trace("Executing getObject() with pid: " + pid);
        return new FedoraObject(readOnlySession
                .getNode(getObjectJcrNodePath(pid)));
    }

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    public Set<String> getObjectNames() throws RepositoryException {

        Node objects = readOnlySession.getNode(getObjectJcrNodePath(""));
        Builder<String> b = builder();
        for (NodeIterator i = objects.getNodes(); i.hasNext();) {
            b.add(i.nextNode().getName());
        }
        return b.build();

    }


    /**
     *
     * @return a double of the size of the fedora:datastream binary content
     * @throws RepositoryException
     */
    public double getAllObjectsDatastreamSize() throws RepositoryException {

        double sum = 0;
        javax.jcr.query.QueryManager queryManager = readOnlySession.getWorkspace().getQueryManager();

        String querystring = "\n" +
                "SELECT [fedora:size] FROM [fedora:checksum]";

        String language = javax.jcr.query.Query.JCR_SQL2;

        javax.jcr.query.Query query = queryManager.createQuery(querystring,language);

        QueryResult queryResults = query.execute();

        final RowIterator rows = queryResults.getRows();
        while(rows.hasNext()) {
            final Row row = rows.nextRow();
            final Value value = row.getValue("fedora:size");

            sum = sum + value.getDouble();
        }

        return sum;
    }

    @PostConstruct
    public final void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public final void logoutSession() {
        readOnlySession.logout();
    }

    public void setRepository(Repository repository) {
        if(readOnlySession != null) {
            logoutSession();
        }
        repo = repository;

        getSession();
    }

}
