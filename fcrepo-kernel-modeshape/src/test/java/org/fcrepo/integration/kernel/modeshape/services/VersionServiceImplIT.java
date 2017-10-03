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
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.VersionService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
public class VersionServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    @Inject
    VersionService versionService;

    @Test
    public void testDummy() {
    }
}
