
package org.fcrepo.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.fcrepo.metrics.RegistryService;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceChangedStatementListener;
import org.fcrepo.utils.NamespaceTools;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;

public class RepositoryService extends JcrTools implements FedoraJcrTypes {

    private static final Logger logger = getLogger(RepositoryService.class);

    public static final MetricRegistry metrics = RegistryService.getMetrics();

    private final Timer objectSizeCalculationTimer = metrics.timer(name(
            RepositoryService.class, "objectSizeCalculation"));

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
    public boolean exists(final Session session, final String path)
            throws RepositoryException {
        return session.nodeExists(path);
    }

    public boolean isFile(final Session session, final String path)
            throws RepositoryException {
        return session.getNode(path).isNodeType("nt:file");
    }

    public Long getRepositorySize() {
        try {

            final Timer.Context context = objectSizeCalculationTimer.time();
            logger.info("Calculating repository size from index");

            try {
                return FedoraTypesUtils.getRepositorySize(repo);

            } finally {
                context.stop();
            }
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

    public static Map<String, String> getRepositoryNamespaces(
            final Session session) throws RepositoryException {
        final NamespaceRegistry reg =
                NamespaceTools.getNamespaceRegistry(session);
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

    public GraphStore getNamespaceRegistryGraph(final Session session) throws RepositoryException {

        final Model model = JcrRdfTools.getJcrNamespaceModel(session);

        model.register(new NamespaceChangedStatementListener(session));

        final GraphStore graphStore = GraphStoreFactory.create(model);

        return graphStore;

    }
    public GraphStore searchRepository(final GraphSubjects subjectFactory,
            final Resource searchSubject, final Session session,
            final String terms, final int limit, final long offset)
            throws RepositoryException {

        final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

        final javax.jcr.query.qom.QueryObjectModelFactory factory =
                queryManager.getQOMFactory();

        final javax.jcr.query.qom.Source selector =
                factory.selector(FEDORA_RESOURCE, "resourcesSelector");
        final javax.jcr.query.qom.Constraint constraints =
                factory.fullTextSearch("resourcesSelector", null, factory
                        .literal(session.getValueFactory().createValue(terms)));

        final javax.jcr.query.Query query =
                factory.createQuery(selector, constraints, null, null);

        query.setLimit(limit + 1); // n+1 for pagination
        query.setOffset(offset);

        final QueryResult queryResult = query.execute();

        final NodeIterator nodeIterator = queryResult.getNodes();
        final long size = nodeIterator.getSize();
        final Iterator<Node> limitedIterator =
                Iterators.limit(
                        new org.fcrepo.utils.NodeIterator(nodeIterator), limit);

        final Model model =
                JcrRdfTools.getJcrNodeIteratorModel(subjectFactory,
                        limitedIterator, searchSubject);

        /* add the result description to the RDF model */

        model.add(
                searchSubject,
                model.createProperty("http://a9.com/-/spec/opensearch/1.1/totalResults"),
                model.createTypedLiteral(size));

        model.add(
                searchSubject,
                model.createProperty("http://a9.com/-/spec/opensearch/1.1/itemsPerPage"),
                model.createTypedLiteral(limit));
        model.add(
                searchSubject,
                model.createProperty("http://a9.com/-/spec/opensearch/1.1/startIndex"),
                model.createTypedLiteral(offset));
        model.add(
                searchSubject,
                model.createProperty("http://a9.com/-/spec/opensearch/1.1/Query#searchTerms"),
                terms);

        if (nodeIterator.hasNext()) {
            model.add(searchSubject, model
                    .createProperty("info:fedora/search/hasMoreResults"), model
                    .createTypedLiteral(true));
        }

        final GraphStore graphStore = GraphStoreFactory.create(model);

        return graphStore;

    }
}
