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
package org.fcrepo.integration.kernel.modeshape;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.TombstoneImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import static org.fcrepo.kernel.modeshape.TombstoneImpl.hasMixin;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author osmandin
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class TombstoneImplIT extends AbstractIT {

    @Inject
    FedoraRepository repo;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    private FedoraSession session;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testTombstoneNodeTypeException() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        session.expire();
        hasMixin(getJcrNode(container));
    }

    @Test
    public void testDeleteObject() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        final TombstoneImpl tombstone = new TombstoneImpl(getJcrNode(container));
        tombstone.delete();

        try {
            nodeService.find(session, container.getPath());
            fail();
        } catch (final RepositoryRuntimeException e) {
            //ok
        }
    }

    @Test
    public void testTombstoneMessage() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final TombstoneImpl tombstone = new TombstoneImpl(getJcrNode(container));

        final String msg = tombstone.toString();
        assertFalse("Msg should not contain 'jcr:': " + msg, msg.contains("jcr:"));
        assertTrue("Msg should contain id: " + pid + " : " + msg, msg.contains(pid));
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testDeleteObjectWithException() throws RepositoryException {
        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid + "/a");
        session.commit();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        final TombstoneImpl tombstone = new TombstoneImpl(getJcrNode(container));
        session.expire();
        tombstone.delete();
    }

}
