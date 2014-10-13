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
import static com.hp.hpl.jena.query.DatasetFactory.create;
import static org.fcrepo.kernel.impl.services.ServiceHelpers.getRepositoryCount;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.NamespaceRdfContext;
import org.fcrepo.metrics.RegistryService;

import java.io.File;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.impl.utils.NamespaceChangedStatementListener;
import org.fcrepo.kernel.services.RepositoryService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RepositoryManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Service for repository-wide management and querying
 *
 * @author Chris Beer
 * @since Mar 11, 2013
 */
@Component
public class RepositoryServiceImpl extends AbstractService implements RepositoryService {

    @Inject
    private Repository repo;

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
     * org.fcrepo.kernel.services.RepositoryService#getNamespaceRegistryDataset
     * (javax.jcr.Session)
     */
    @Override
    public Dataset getNamespaceRegistryDataset(final Session session,
                                               final IdentifierConverter<Resource,Node> idTranslator) {


        final Model model;

        try {
            model = getNamespaceRegistryStream(session, idTranslator).asModel();

            model.register(new NamespaceChangedStatementListener(session));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return create(model);

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#getNamespaceRegistryStream
     * (javax.jcr.Session)
     */
    @Override
    public RdfStream getNamespaceRegistryStream(final Session session,
                                                final IdentifierConverter<Resource,Node> idTranslator) {

        try {
            return new NamespaceRdfContext(session);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#backupRepository(javax.jcr
     * .Session, java.io.File)
     */
    @Override
    public Problems backupRepository(final Session session,
                                     final File backupDirectory) {
        try {
            final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                    .getWorkspace()
                    .getRepositoryManager();

            return repoMgr.backupRepository(backupDirectory);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.services.RepositoryService#restoreRepository(javax.
     * jcr.Session, java.io.File)
     */
    @Override
    public Problems restoreRepository(final Session session,
                                      final File backupDirectory) {
        try {
            final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) session)
                    .getWorkspace()
                    .getRepositoryManager();

            return repoMgr.restoreRepository(backupDirectory);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

}
