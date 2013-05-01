
package org.fcrepo.services;

import static com.google.common.base.Throwables.propagate;
import static javax.jcr.query.Query.JCR_SQL2;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
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

    private static final Logger logger = LoggerFactory
            .getLogger(RepositoryService.class);

    public static final MetricRegistry metrics = RegistryService.getMetrics();

    @Inject
    protected Repository repo;


    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(final PrintStream os) {
        RegistryService.dumpMetrics(os);
    }
    
    /**
     * 
     * @param path
     * @return whether a node exists at the given path
     * @throws RepositoryException
     */
    public boolean exists(Session session, String path) throws RepositoryException {
        return session.nodeExists(path);
    }
    
    public boolean isFile(Session session, String path) throws RepositoryException {
        return session.getNode(path).isNodeType("nt:file");
    }

    /**
    *
    * @return a double of the size of the fedora:datastream binary content
    * @throws RepositoryException
    */
    public long getAllObjectsDatastreamSize() throws RepositoryException {

		final Session session = repo.login();
        long sum = 0;
        final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

        final String querystring =
                "\n" + "SELECT [" + FEDORA_SIZE + "] FROM [" + FEDORA_CHECKSUM +
                        "]";

        final QueryResult queryResults =
                queryManager.createQuery(querystring, JCR_SQL2).execute();

        for (final RowIterator rows = queryResults.getRows(); rows.hasNext();) {
            final Value value = rows.nextRow().getValue(FEDORA_SIZE);
            sum += value.getLong();
        }

		session.logout();

        return sum;
    }

    public Long getRepositorySize() {
        try {
            return getAllObjectsDatastreamSize();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    public Long getRepositoryObjectCount(final Session session) {
        try {
            return session.getNode("/objects").getNodes().getSize();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    public NodeTypeIterator getAllNodeTypes(final Session session)
            throws RepositoryException {
        final NodeTypeManager ntmanager =
                (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
        return ntmanager.getAllNodeTypes();
    }

    public Map<String, String> getRepositoryNamespaces(final Session session)
            throws RepositoryException {
        final NamespaceRegistry reg =
                session.getWorkspace().getNamespaceRegistry();
        final String[] prefixes = reg.getPrefixes();
        final HashMap<String, String> result =
                new HashMap<String, String>(prefixes.length);
        for (final String prefix : reg.getPrefixes()) {
            result.put(prefix, reg.getURI(prefix));
        }
        return result;
    }

    public void setRepository(final Repository repository) {
        repo = repository;
    }

}
