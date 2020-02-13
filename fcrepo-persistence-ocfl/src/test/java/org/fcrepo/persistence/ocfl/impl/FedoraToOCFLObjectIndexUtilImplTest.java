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
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

import static java.lang.System.currentTimeMillis;
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

    @Test
    public void testRebuild() throws Exception {
        final var targetDir = new File("target");
        final var dataDir = new File(targetDir, "test-fcrepo-data-" + currentTimeMillis());
        final var repoDir = new File(dataDir,"ocfl-repo");
        final var workDir = new File(dataDir,"ocfl-work");
        final var staging = new File(dataDir,"ocfl-staging");

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
        final var resource2 =  resource1 + "/resource2";

        final var session = sessionManager.getSession(session1Id);

        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(resource1);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)operation).isArchivalGroup()).thenReturn(true);
        session.persist(operation);

        final var operation2 = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation2.getResourceId()).thenReturn(resource2);
        when(operation2.getType()).thenReturn(CREATE);
        final var bytes = "test".getBytes();
        final var stream = new ByteArrayInputStream(bytes);
        when(operation2.getContentSize()).thenReturn((long)bytes.length);
        when(operation2.getContentStream()).thenReturn(stream);
        when(operation2.getMimeType()).thenReturn("text/plain");
        when(operation2.getFilename()).thenReturn("test");
        when(((CreateResourceOperation)operation2).getParentId()).thenReturn(resource1);
        session.persist(operation2);
        session.commit();
        assertNotNull(index.getMapping(resource1));
        index.reset();

        try {
            index.getMapping(resource1);
            fail(resource1 + " should not exist in index");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            //do nothing - expected
        }

        util.rebuild();
        assertNotNull(index.getMapping(resource1));
        assertNotNull(index.getMapping(resource2));
    }
}