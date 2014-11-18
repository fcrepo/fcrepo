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
package org.fcrepo.integration.kernel.impl;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.TombstoneImpl;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.services.ContainerService;
import org.fcrepo.kernel.services.NodeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator(session);
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testTombstoneNodeTypeException() throws RepositoryException {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        new TombstoneImpl(container.getNode());
        session.logout();
        TombstoneImpl.hasMixin(container.getNode());
    }

    @Test
    public void testDeleteObject() throws RepositoryException {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        final TombstoneImpl tombstone = new TombstoneImpl(container.getNode());
        tombstone.delete();

        try {
            nodeService.find(session, container.getPath());
            fail();
        } catch (RepositoryRuntimeException e) {
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
