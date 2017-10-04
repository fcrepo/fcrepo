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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TIME_MAP;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.FedoraTimeMapImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>FedoraTimeMapImplIT class.</p>
 *
 * @author lsitu
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraTimeMapImplIT extends AbstractIT {

    @Inject
    FedoraRepository repo;

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

    @Test
    public void testGetTimeMapLDPCv() throws RepositoryException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // Create TimeMap (LDPCv)
        final FedoraResource ldpcvResource = resource.findOrCreateTimeMap();

        assertNotNull(ldpcvResource);
        assertEquals("/" + pid + "/" + LDPCV_TIME_MAP, ldpcvResource.getPath());

        session.commit();

        final javax.jcr.Node timeMapNode = jcrSession.getNode("/" + pid).getNode(LDPCV_TIME_MAP);
        assertTrue(timeMapNode.isNodeType(FEDORA_TIME_MAP));

        final FedoraTimeMapImpl fedoraTimeMap = new FedoraTimeMapImpl(getJcrNode(ldpcvResource));
        assertEquals(timeMapNode, fedoraTimeMap.getNode());
    }
}
