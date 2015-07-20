/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.modeshape;

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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.modeshape.TombstoneImpl.hasMixin;
import static org.junit.Assert.fail;

/**
 * @author osmandin
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class TombstoneImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    private Session session;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testTombstoneNodeTypeException() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        session.logout();
        hasMixin(container.getNode());
    }

    @Test
    public void testDeleteObject() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        final TombstoneImpl tombstone = new TombstoneImpl(container.getNode());
        tombstone.delete();

        try {
            nodeService.find(session, container.getPath());
            fail();
        } catch (final RepositoryRuntimeException e) {
            //ok
        }
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testDeleteObjectWithException() throws RepositoryException {
        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid + "/a");
        session.save();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        final TombstoneImpl tombstone = new TombstoneImpl(container.getNode());
        session.logout();
        tombstone.delete();
    }

}
