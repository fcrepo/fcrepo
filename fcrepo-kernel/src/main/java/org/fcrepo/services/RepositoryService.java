package org.fcrepo.services;

import static javax.jcr.query.Query.JCR_SQL2;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.fcrepo.metrics.RegistryService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.MetricRegistry;

public class RepositoryService extends JcrTools implements FedoraJcrTypes {
    final private Logger logger = LoggerFactory
            .getLogger(RepositoryService.class);

    public static final MetricRegistry metrics = RegistryService.getMetrics();

    @Inject
    protected Repository repo;

    /**
     * For use with non-mutating methods.
     */
    protected Session readOnlySession;


    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(PrintStream os) {
        RegistryService.dumpMetrics(os);
    }

    /**
     * Alter the total repository size.
     * 
     * @param change
     *            the amount by which to [de|in]crement the total repository
     *            size
     * @param session
     *            the javax.jcr.Session in which the originating mutation is
     *            occurring
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public void updateRepositorySize(Long change, Session session)
            throws PathNotFoundException, RepositoryException {
        try {
        logger.debug("updateRepositorySize called with change quantity: " +
                change);

        final Node objectStore = findOrCreateNode(session, "/objects");


        Property sizeProperty = objectStore.getProperty(FEDORA_SIZE);

        Long previousSize = sizeProperty.getLong();
        logger.debug("Previous repository size: " + previousSize);
        synchronized (sizeProperty) {
            sizeProperty.setValue(previousSize + change);
            session.save();
        }
        logger.debug("Current repository size: " + sizeProperty.getLong());
        } catch(RepositoryException e) {
            logger.warn(e.toString());
            throw e;
        }
    }

    /**
    *
    * @return a double of the size of the fedora:datastream binary content
    * @throws RepositoryException
    */
   public long getAllObjectsDatastreamSize() throws RepositoryException {

       long sum = 0;
       QueryManager queryManager =
               readOnlySession.getWorkspace().getQueryManager();

       final String querystring =
               "\n" + "SELECT [" + FEDORA_SIZE + "] FROM [" + FEDORA_CHECKSUM +
                       "]";

       final QueryResult queryResults =
               queryManager.createQuery(querystring, JCR_SQL2).execute();

       for (final RowIterator rows = queryResults.getRows(); rows.hasNext();) {
           final Value value = rows.nextRow().getValue(FEDORA_SIZE);
           sum += value.getLong();
       }

       return sum;
   }
   
    public Long getRepositorySize(Session session) {
        try {
            return getAllObjectsDatastreamSize();
        } catch(RepositoryException e) {
            logger.warn(e.toString());
            return -1L;
        }
    }

    public Long getRepositoryObjectCount(Session session) {
        try {
            return session.getNode("/objects").getNodes().getSize();
        } catch(RepositoryException e) {
            logger.warn(e.toString());
            return -1L;
        }
    }
    
    public NodeTypeIterator getAllNodeTypes(Session session) throws RepositoryException {
        final NodeTypeManager ntmanager =
                (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
        return ntmanager.getAllNodeTypes();
    }
    
    public Map<String, String> getRepositoryNamespaces(Session session) throws RepositoryException {
        final NamespaceRegistry reg =
                session.getWorkspace().getNamespaceRegistry();
        String [] prefixes = reg.getPrefixes();
        HashMap<String, String> result = new HashMap<String, String>(prefixes.length);
        for (final String prefix : reg.getPrefixes()) {
            result.put(prefix, reg.getURI(prefix));
        }
        return result;
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
        if (readOnlySession != null) {
            logoutSession();
        }
        repo = repository;
        getSession();
    }

}
