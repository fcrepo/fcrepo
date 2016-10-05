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

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.services.BatchService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
public class BatchServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    @Inject
    BatchService batchService;

    @Test
    public void testGetService() throws RepositoryException {
        final FedoraSession session = repository.login();
        try {
            batchService.begin(session);
            assertTrue(batchService.exists(session.getId()));
            assertTrue(batchService.exists(session.getId(), null));
            assertFalse(batchService.exists(session.getId(), "other-user"));

            final FedoraSession t2 = batchService.getSession(session.getId());
            assertNotNull(t2);
            assertEquals(session.getId(), t2.getId());
        } finally {
            session.expire();
        }
    }

    @Test
    public void testGetBatchUser() throws RepositoryException {
        final FedoraSession session = repository.login();
        try {
            batchService.begin(session);
            assertTrue(batchService.exists(session.getId()));
            assertTrue(batchService.exists(session.getId(), null));
            assertFalse(batchService.exists(session.getId(), "other-user"));

            final FedoraSession t2 = batchService.getSession(session.getId());
            assertNotNull(t2);
            assertEquals(session.getId(), t2.getId());
        } finally {
            session.expire();
        }
    }

    @Test
    public void testBatchExpire() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraSession session = repository.login();
        try {
            batchService.begin(session);
            containerService.findOrCreate(session, "/" + pid);

            // rollback and make sure the object doesn't exist
            session.expire();
            batchService.removeExpired();
        } finally {
            session.expire();
        }
        final FedoraSession session2 = repository.login();
        try {
            assertFalse(nodeService.exists(session2, "/" + pid));
        } finally {
            session2.expire();
        }
    }

    @Test
    public void testAbort() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraSession session = repository.login();
        try {
            batchService.begin(session);
            containerService.findOrCreate(session, "/" + pid);

            // rollback and make sure the object doesn't exist
            batchService.abort(session.getId());
            session.commit();
        } finally {
            session.expire();
        }
        final FedoraSession session2 = repository.login();
        try {
            assertFalse(nodeService.exists(session2, "/" + pid));
        } finally {
            session2.expire();
        }
    }

    @Test
    public void testCommit() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraSession session = repository.login();
        try {
            batchService.begin(session);
            containerService.findOrCreate(session, "/" + pid);

            // rollback and make sure the object doesn't exist
            batchService.commit(session.getId());
            session.commit();

        } finally {
            session.expire();
        }
        final FedoraSession session2 = repository.login();
        try {
            assertTrue(nodeService.exists(session2, "/" + pid));
        } finally {
            session2.expire();
        }
    }
}
