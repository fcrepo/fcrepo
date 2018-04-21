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

import static org.fcrepo.kernel.modeshape.FedoraWebacAclImpl.hasMixin;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>FedoraWebAclImplIT class.</p>
 *
 * @author lsitu
 */
@Ignore
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraWebacAclImplIT extends AbstractIT {

    @Inject
    FedoraRepository repo;

    @Inject
    ContainerService containerService;

    private FedoraSession session;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testFedoraWebacAclNodeTypeException() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid + "/a");
        session.expire();
        hasMixin(getJcrNode(container));
    }
}
