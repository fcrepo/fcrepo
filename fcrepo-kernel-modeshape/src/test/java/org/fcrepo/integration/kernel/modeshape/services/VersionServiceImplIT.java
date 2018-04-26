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
package org.fcrepo.integration.kernel.modeshape.services;

import java.time.Instant;
import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
@Ignore("Until implemented with Memento")
public class VersionServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    @Inject
    VersionService versionService;

    private DefaultIdentifierTranslator subjects;

    private FedoraSession session;

    private static final Instant mementoDate1 = Instant.now();

    private static final Instant mementoDate2 = Instant.from(LocalDateTime.of(2000, 5, 10, 18, 30));

    @Before
    public void setUp() throws RepositoryException {
        session = repository.login();
        subjects = new DefaultIdentifierTranslator(getJcrSession(session));
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test
    public void testCreateVersion() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();
        resource.findOrCreateTimeMap();
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, resource, subjects, mementoDate1);
        session.commit();
        assertEquals(1L, countVersions(session, resource));
    }

    @Test
    public void testRemoveVersion() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, resource, subjects, mementoDate1);
        session.commit();
        assertEquals(1L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session, resource, subjects, mementoDate2);
        session.commit();
        assertEquals(2L, countVersions(session, resource));
    }

    private static long countVersions(final FedoraSession session, final FedoraResource resource )
            throws RepositoryException {
        final FedoraResource timeMap = resource.findOrCreateTimeMap();
        return timeMap.getChildren().count();
    }
}
