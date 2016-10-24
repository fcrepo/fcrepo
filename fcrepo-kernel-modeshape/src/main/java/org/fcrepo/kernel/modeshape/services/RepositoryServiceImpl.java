/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services;

import static com.codahale.metrics.MetricRegistry.name;
import static org.fcrepo.kernel.modeshape.FedoraRepositoryImpl.getJcrRepository;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.services.ServiceHelpers.getRepositoryCount;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.metrics.RegistryService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;


import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.services.RepositoryService;
import org.modeshape.jcr.api.RepositoryManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer;

/**
 * Service for repository-wide management and querying
 *
 * @author Chris Beer
 * @since Mar 11, 2013
 */
@Component
public class RepositoryServiceImpl extends AbstractService implements RepositoryService {

    @Inject
    private FedoraRepository repository;

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

            LOGGER.debug("Calculating repository size from index");
            final Repository repo = getJcrRepository(repository);

            try (final Timer.Context context = objectSizeCalculationTimer.time()) {
                // Differentiating between the local getRepositorySize and
                // ServiceHelpers
                return ServiceHelpers.getRepositorySize(repo);

            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.services.RepositoryService#getRepositoryObjectCount()
     */
    @Override
    public Long getRepositoryObjectCount() {
        final Repository repo = getJcrRepository(repository);
        try {
            return getRepositoryCount(repo);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.services.RepositoryService#backupRepository(javax.jcr
     * .Session, java.io.File)
     */
    @Override
    public Collection<Throwable> backupRepository(final FedoraSession session,
                                     final File backupDirectory) {
        final Session jcrSession = getJcrSession(session);
        try {
            final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) jcrSession)
                    .getWorkspace()
                    .getRepositoryManager();

            final Collection<Throwable> problems = new ArrayList<>();

            repoMgr.backupRepository(backupDirectory).forEach(x -> problems.add(x.getThrowable()));

            return problems;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.services.RepositoryService#restoreRepository(javax.
     * jcr.Session, java.io.File)
     */
    @Override
    public Collection<Throwable> restoreRepository(final FedoraSession session,
                                      final File backupDirectory) {
        final Session jcrSession = getJcrSession(session);
        try {
            final RepositoryManager repoMgr = ((org.modeshape.jcr.api.Session) jcrSession)
                    .getWorkspace()
                    .getRepositoryManager();

            final Collection<Throwable> problems = new ArrayList<>();

            repoMgr.restoreRepository(backupDirectory).forEach(x -> problems.add(x.getThrowable()));

            return problems;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

}
