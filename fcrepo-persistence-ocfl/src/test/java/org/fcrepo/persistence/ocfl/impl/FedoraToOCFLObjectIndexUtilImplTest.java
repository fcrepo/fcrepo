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
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.createRepository;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class FedoraToOCFLObjectIndexUtilImplTest {

    private static final String RESOURCE_ID_1 = "info:fedora/resource";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRebuild() throws Exception {

        final var repoDir = tempFolder.newFolder("ocfl-repo");
        final var workDir = tempFolder.newFolder("ocfl-work");
        final var staging = tempFolder.newFolder("ocfl-staging");
        final var repository = createRepository(repoDir, workDir);

        final var index = new FedoraToOCFLObjectIndexImpl();

        final var ocflObjectSessionFactory = new DefaultOCFLObjectSessionFactory(staging);
        setField(ocflObjectSessionFactory, "ocflRepository", repository);

        final var sessionManager = new OCFLPersistentSessionManager();
        setField(sessionManager, "fedoraOcflIndex", index);
        setField(sessionManager, "objectSessionFactory", ocflObjectSessionFactory);

        final var util = new FedoraToOCFLObjectIndexUtilImpl();
        setField(util, "ocflRepository", repository);
        setField(util, "fedoraToOCFLObjectIndex", index);
        setField(util, "objectSessionFactory", ocflObjectSessionFactory);

        final var session1Id = "session1";
        final var resource1 = "info:fedora/resource1";
        final var session = sessionManager.getSession(session1Id);

        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(resource1);
        when(operation.getType()).thenReturn(CREATE);
        session.persist(operation);
        session.commit();

        assertNotNull(index.getMapping(resource1));

        index.reset();

        try {
            index.getMapping(resource1);
            fail(resource1 + " should not exist in index");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            //do nothing - expected
        }

        ocflObjectSessionFactory.create(resource1, session1Id);

        util.rebuild();

        assertNotNull(index.getMapping(resource1));

    }
}