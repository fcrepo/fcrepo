/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.metrics.RegistryService.getMetrics;

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

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceChangedStatementListener;
import org.fcrepo.utils.NamespaceTools;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import com.codahale.metrics.Timer;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 11, 2013
 */
public class RepositoryService extends JcrTools implements FedoraJcrTypes {

    private static final Logger logger = getLogger(RepositoryService.class);


    private final Timer objectSizeCalculationTimer =
        getMetrics().timer(name(RepositoryService.class, "objectSizeCalculation"));

    @Inject
    protected Repository repo;

    /**
     * Test whether a node exists in the JCR store
     *
     * @param path
     * @return whether a node exists at the given path
     * @throws RepositoryException
     */
    public boolean exists(final Session session, final String path)
        throws RepositoryException {
        return session.nodeExists(path);
    }

    /**
     * Calculate the total size of all the binary properties in the repository
     *
     * @return size in bytes
     */
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

    /**
     * Calculate the number of objects in the repository
     *
     * @return
     */
    public Long getRepositoryObjectCount() {
        try {
            return FedoraTypesUtils.getRepositoryCount(repo);
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /**
     * Get the full list of node types in the repository
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public NodeTypeIterator getAllNodeTypes(final Session session)
        throws RepositoryException {
        final NodeTypeManager ntmanager =
            (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
        return ntmanager.getAllNodeTypes();
    }

    /**
     * Get a map of JCR prefixes to their URI namespaces
     * @param session
     * @return
     * @throws RepositoryException
     */
    public static Map<String, String> getRepositoryNamespaces(final Session session)
        throws RepositoryException {

        final NamespaceRegistry reg =
            NamespaceTools.getNamespaceRegistry(session);
        final String[] prefixes = reg.getPrefixes();
        final Map<String, String> result =
            new HashMap<String, String>(prefixes.length);
        for (final String prefix : reg.getPrefixes()) {
            result.put(prefix, reg.getURI(prefix));
        }
        return result;
    }

    /**
     * Serialize the JCR namespace information as an RDF Dataset
     * @param session
     * @return
     * @throws RepositoryException
     */
    public Dataset getNamespaceRegistryGraph(final Session session)
        throws RepositoryException {

        final Model model = JcrRdfTools.getJcrNamespaceModel(session);

        model.register(new NamespaceChangedStatementListener(session));

        final Dataset dataset = DatasetFactory.create(model);

        return dataset;

    }

    /**
     * Perform a full-text search on the whole repository and return the
     * information as an RDF Dataset
     *
     * @param subjectFactory
     * @param searchSubject RDF resource to use as the subject of the search
     * @param session
     * @param terms
     * @param limit
     * @param offset
     * @return
     * @throws RepositoryException
     */
    public Dataset searchRepository(final GraphSubjects subjectFactory,
                                    final Resource searchSubject,
                                    final Session session,
                                    final String terms,
                                    final int limit,
                                    final long offset)
        throws RepositoryException {

        final QueryManager queryManager =
            session.getWorkspace().getQueryManager();

        final javax.jcr.query.qom.QueryObjectModelFactory factory =
            queryManager.getQOMFactory();

        final javax.jcr.query.qom.Source selector =
            factory.selector(FEDORA_RESOURCE, "resourcesSelector");
        final javax.jcr.query.qom.Constraint constraints =
            factory.fullTextSearch("resourcesSelector", null, factory
                                   .literal(session.getValueFactory()
                                            .createValue(terms)));

        final javax.jcr.query.Query query =
            factory.createQuery(selector, constraints, null, null);

        // include an extra document to determine if additional pagination is
        // necessary
        query.setLimit(limit + 1);
        query.setOffset(offset);

        final QueryResult queryResult = query.execute();

        final NodeIterator nodeIterator = queryResult.getNodes();
        final long size = nodeIterator.getSize();

        // remove that extra document from the nodes we'll iterate over
        final Iterator<Node> limitedIterator =
            Iterators.limit(new org.fcrepo.utils.NodeIterator(nodeIterator),
                            limit);

        final Model model =
            JcrRdfTools.getJcrNodeIteratorModel(subjectFactory,
                                                limitedIterator, searchSubject);

        /* add the result description to the RDF model */

        model.add(
                  searchSubject,
                  model.createProperty("http://a9.com/-/spec/opensearch/1.1/" +
                                       "totalResults"),
                  model.createTypedLiteral(size));

        model.add(
                  searchSubject,
                  model.createProperty("http://a9.com/-/spec/opensearch/1.1/" +
                                       "itemsPerPage"),
                  model.createTypedLiteral(limit));
        model.add(
                  searchSubject,
                  model.createProperty("http://a9.com/-/spec/opensearch/1.1/" +
                                       "startIndex"),
                  model.createTypedLiteral(offset));
        model.add(
                  searchSubject,
                  model.createProperty("http://a9.com/-/spec/opensearch/1.1/" +
                                       "Query#searchTerms"),
                  terms);

        if (nodeIterator.hasNext()) {
            model.add(searchSubject,
                      model.createProperty("info:fedora/search/hasMoreResults"),
                      model.createTypedLiteral(true));
        }

        final Dataset dataset = DatasetFactory.create(model);

        return dataset;

    }

    /**
     * @todo Add Documentation.
     */
    public void setRepository(final Repository repository) {
        repo = repository;
    }
}
