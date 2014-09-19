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
package org.fcrepo.integration.kernel.impl.services;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import static org.junit.Assert.assertFalse;

/**
 * @author cabeer
 */

@ContextConfiguration({"/spring-test/repo.xml"})
public class NodeServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ObjectService objectService;



    @Test
    public void testDeleteObject() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getRandomPid();
        nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        nodeService.deleteObject(session, "/" + pid);
        session.save();

        assertFalse(session.nodeExists("/" + pid));
    }

    @Test
    public void testDeleteObjectWithInboundReferences() throws RepositoryException {

        final Session session = repository.login();
        final String pid = getRandomPid();
        final FedoraResource resourceA = objectService.createObject(session, "/" + pid + "/a");
        final FedoraResource resourceB = objectService.createObject(session, "/" + pid + "/b");

        final Value value = session.getValueFactory().createValue(resourceB.getNode());
        resourceA.getNode().setProperty("fedorarelsext:hasMember", new Value[] { value });

        session.save();
        nodeService.deleteObject(session, "/" + pid + "/a");
        session.save();

        nodeService.deleteObject(session, "/" + pid + "/b");
        session.save();

        assertFalse(session.nodeExists("/" + pid + "/b"));

    }

    @Test(expected = FedoraInvalidNamespaceException.class)
    public void testExistsWithBadNamespace() throws RepositoryException {
        final Session session = repository.login();
        final String path = "/bad_ns: " + getRandomPid();

        objectService.exists(session, path);
    }
}
