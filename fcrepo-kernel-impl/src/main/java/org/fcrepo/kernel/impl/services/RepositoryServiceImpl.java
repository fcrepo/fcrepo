/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterators.limit;
import static com.google.common.collect.Maps.asMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.hp.hpl.jena.query.DatasetFactory.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_MORE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_TOTAL_RESULTS;
import static org.fcrepo.kernel.impl.services.ServiceHelpers.getRepositoryCount;
import static org.slf4j.LoggerFactory.getLogger;
import org.fcrepo.metrics.RegistryService;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;

import org.fcrepo.kernel.rdf.GraphProperties;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.impl.utils.NamespaceChangedStatementListener;
import org.fcrepo.kernel.services.RepositoryService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RepositoryManager;
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
 * Service for repository-wide management and querying
 *
 * @author Chris Beer
 * @since Mar 11, 2013
 */
@Component
public class RepositoryServiceImpl extends AbstractService implements RepositoryService {

    private static final Logger LOGGER = getLogger(RepositoryServiceImpl.class);

    private final Timer objectSizeCalculationTimer = RegistryService.getInstance().getMetrics().timer(
            name(RepositoryService.class, "objectSizeCalculation"));

    /**
     * Calculate the total size of all the binary properties in the repository
     *
     * @return size in bytes
     */
    @Override
    public Long getRepositorySize() {
        try {

            final Timer.Context context = objectSizeCalculationTimer.time();
            LOGGER.debug("Calculating repository size from index");

            try {
                // Differentiating between the local getRepositorySize and
                // ServiceHelpers
                return ServiceHelpers.getRepositorySize(repo);

            } finally {
                context.stop();
            }
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#getRepositoryObjectCount()
     */
    @Override
    public Long getRepositoryObjectCount() {
        try {
            return getRepositoryCount(repo);
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#getRepositoryNamespaces(
     * javax.jcr.Session)
     */
    @Override
    public Map<String, String> getRepositoryNamespaces(final Session session) throws RepositoryException {

        final NamespaceRegistry reg = session.getWorkspace().getNamespaceRegistry();
        return asMap(newHashSet(reg.getPrefixes()), new Function<String, String>() {

            @Override
            public String apply(final String p) {
                try {
                    return reg.getURI(p);
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#getNamespaceRegistryDataset
     * (javax.jcr.Session)
     */
    @Override
    public Dataset getNamespaceRegistryDataset(final Session session, final IdentifierTranslator idTranslator)
        throws RepositoryException {

        final Model model =
            JcrRdfTools.withContext(idTranslator, session).getNamespaceTriples().asModel();

        model.register(new NamespaceChangedStatementListener(session));

        final Dataset dataset = create(model);

        return dataset;

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#getNamespaceRegistryStream
     * (javax.jcr.Session)
     */
    @Override
    public RdfStream getNamespaceRegistryStream(final Session session, final IdentifierTranslator idTranslator)
        throws RepositoryException {

        return JcrRdfTools.withContext(idTranslator, session).getNamespaceTriples();

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#searchRepository(org.fcrepo
     * .kernel.rdf.GraphSubjects, com.hp.hpl.jena.rdf.model.Resource,
     * javax.jcr.Session, java.lang.String, int, long)
     */
    @Override
    public Dataset searchRepository(final IdentifierTranslator subjectFactory,
            final Resource searchSubject, final Session session,
            final String terms, final int limit, final long offset)
        throws RepositoryException {

        final Model model;

        if (terms != null && terms.trim().length() != 0) {
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

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#backupRepository(javax.jcr
     * .Session, java.io.File)
     */
    @Override
    public Problems backupRepository(final Session session,
                                     final File backupDirectory) throws RepositoryException {
        final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                .getWorkspace()
                .getRepositoryManager();

        final Problems problems = repoMgr.backupRepository(backupDirectory);

        return problems;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#restoreRepository(javax.
     * jcr.Session, java.io.File)
     */
    @Override
    public Problems restoreRepository(final Session session,
                                      final File backupDirectory) throws RepositoryException {
        final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                .getWorkspace()
                .getRepositoryManager();

        final Problems problems = repoMgr.restoreRepository(backupDirectory);

        return problems;
    }

}
