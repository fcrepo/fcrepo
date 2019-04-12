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

import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;

import javax.inject.Inject;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.services.ContainerService;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * @author whikloj
 */
@ContextConfiguration({ "/spring-test/fcrepo-config.xml" })
public class ContainerServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    private ContainerService containerService;

    @Test
    public void testCreateUndefinedPrefix() throws Exception {
        FedoraSession session = repository.login();
        final String id = getRandomPid();
        final String path = "/new_ns:" + id;
        final String encodedPath = "new_ns%3A" + id;

        assertEquals(false, containerService.exists(session, path));

        containerService.findOrCreate(session, path);

        session.commit();
        session.expire();

        session = repository.login();
        final Session jcrSession = getJcrSession(session);

        assertEquals(true, containerService.exists(session, path));
        assertEquals(true, jcrSession.getRootNode().hasNode(encodedPath));
        session.expire();
    }
}
