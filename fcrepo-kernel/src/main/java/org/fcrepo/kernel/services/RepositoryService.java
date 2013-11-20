/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.kernel.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterators.limit;
import static com.google.common.collect.Maps.asMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_MORE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_TOTAL_RESULTS;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.rdf.GraphProperties;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.rdf.impl.NodeTypeRdfContext;
import org.fcrepo.kernel.utils.FedoraTypesUtils;
import org.fcrepo.kernel.utils.NamespaceChangedStatementListener;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Context;

/**
 * Repository-global helper methods
 *
 * @author Chris Beer
 * @date Mar 11, 2013
 */
@Component
public class RepositoryService extends JcrTools implements FedoraJcrTypes {

    private static final Logger logger = getLogger(RepositoryService.class);

    private final Timer objectSizeCalculationTimer = getMetrics().timer(
            name(RepositoryService.class, "objectSizeCalculation"));

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
            logger.debug("Calculating repository size from index");

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
            return getRepositoryCount(repo);
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
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public static Map<String, String> getRepositoryNamespaces(
            final Session session) throws RepositoryException {

        final NamespaceRegistry reg =
            session.getWorkspace().getNamespaceRegistry();
        return asMap(newHashSet(reg.getPrefixes()),
                new Function<String, String>() {

                    @Override
                    public String apply(final String p) {
                        try {
                            return reg.getURI(p);
                        } catch (final RepositoryException e) {
                            propagate(e);
                        }
                        return null;
                    }
                });
    }

    /**
     * Serialize the JCR namespace information as an RDF Dataset
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public Dataset getNamespaceRegistryGraph(final Session session)
        throws RepositoryException {

        final Model model =
            JcrRdfTools.withContext(null, session).getNamespaceTriples()
                    .asModel();

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
            final Resource searchSubject, final Session session,
            final String terms, final int limit, final long offset)
        throws RepositoryException {

        final Model model;

        if (terms != null) {
            final QueryManager queryManager =
                    session.getWorkspace().getQueryManager();

            final QueryObjectModelFactory factory =
                    queryManager.getQOMFactory();

            final Source selector =
                    factory.selector(FEDORA_RESOURCE, "resourcesSelector");
            final Constraint constraints =
                    factory.fullTextSearch("resourcesSelector", null, factory
                            .literal(session.getValueFactory().createValue(
                                    terms)));

            final Query query =
                    factory.createQuery(selector, constraints, null, null);

            // include an extra document to determine if additional pagination
            // is
            // necessary
            query.setLimit(limit + 1);
            query.setOffset(offset);

            final QueryResult queryResult = query.execute();

            final NodeIterator nodeIterator = queryResult.getNodes();
            final long size = nodeIterator.getSize();

            // remove that extra document from the nodes we'll iterate over
            final Iterator<Node> limitedIterator =
                    limit(new org.fcrepo.kernel.utils.iterators.NodeIterator(nodeIterator),
                            limit);

            model =
                JcrRdfTools.withContext(subjectFactory, session)
                        .getJcrPropertiesModel(limitedIterator, searchSubject)
                        .asModel();

            /* add the result description to the RDF model */

            model.add(searchSubject, SEARCH_HAS_TOTAL_RESULTS, model
                    .createTypedLiteral(size));
            model.add(searchSubject, SEARCH_HAS_MORE, model
                    .createTypedLiteral(nodeIterator.hasNext()));
        } else {
            model = createDefaultModel();
        }

        final Dataset dataset = DatasetFactory.create(model);

        final String uri = searchSubject.getURI();
        final Context context =
                dataset.getContext() != null ? dataset.getContext()
                        : new Context();
        context.set(GraphProperties.URI_SYMBOL, uri);

        return dataset;

    }

    /**
     * This method backups up a running repository
     *
     * @param session
     * @param backupDirectory
     * @return
     * @throws RepositoryException
     */
    public Problems backupRepository(final Session session,
                                     final File backupDirectory) throws RepositoryException {
        final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                .getWorkspace()
                .getRepositoryManager();

        final Problems problems = repoMgr.backupRepository(backupDirectory);

        return problems;
    }

    /**
     * This methods restores the repository from a backup
     *
     * @param session
     * @param backupDirectory
     * @return
     * @throws RepositoryException
     */
    public Problems restoreRepository(final Session session,
                                      final File backupDirectory) throws RepositoryException {
        final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                .getWorkspace()
                .getRepositoryManager();

        final Problems problems = repoMgr.restoreRepository(backupDirectory);

        return problems;
    }

    /**
     * Set the repository to back this RepositoryService
     *
     * @param repository
     */
    public void setRepository(final Repository repository) {
        repo = repository;
    }

    /**
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public RdfStream getNodeTypes(final Session session) throws RepositoryException {
        return new NodeTypeRdfContext(session.getWorkspace().getNodeTypeManager());
    }

    /**
     *
     * @param session
     * @param cndStream
     * @throws RepositoryException
     * @throws IOException
     */
    public void registerNodeTypes(final Session session,
                                  final InputStream cndStream) throws RepositoryException, IOException {
        final NodeTypeManager nodeTypeManager = (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
        nodeTypeManager.registerNodeTypes(cndStream, true);
    }
}
