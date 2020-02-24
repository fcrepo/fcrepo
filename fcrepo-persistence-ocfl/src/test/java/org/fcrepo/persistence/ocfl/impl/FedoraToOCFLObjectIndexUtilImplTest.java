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
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndexUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.createRepository;
import static org.junit.Assert.assertEquals;
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

    private PersistentStorageSessionManager sessionManager;
    private FedoraToOCFLObjectIndex index;
    private FedoraToOCFLObjectIndexUtil util;

    @Before
    public void setup() throws IOException {
        final var targetDir = new File("target");
        final var dataDir = new File(targetDir, "test-fcrepo-data-" + currentTimeMillis());
        final var repoDir = new File(dataDir,"ocfl-repo");
        final var workDir = new File(dataDir,"ocfl-work");
        final var staging = new File(dataDir,"ocfl-staging");

        final var repository = createRepository(repoDir, workDir);

        index = new FedoraToOCFLObjectIndexImpl();
        index.reset();

        final var ocflObjectSessionFactory = new DefaultOCFLObjectSessionFactory(staging);
        setField(ocflObjectSessionFactory, "ocflRepository", repository);

        sessionManager = new OCFLPersistentSessionManager();
        setField(sessionManager, "fedoraOcflIndex", index);
        setField(sessionManager, "objectSessionFactory", ocflObjectSessionFactory);

        util = new FedoraToOCFLObjectIndexUtilImpl();
        setField(util, "ocflRepository", repository);
        setField(util, "fedoraToOCFLObjectIndex", index);
        setField(util, "objectSessionFactory", ocflObjectSessionFactory);
    }

    @Test
    public void rebuildWhenRepoContainsArchivalGroupObject() throws Exception {
        final var session1Id = "session1";
        final var resource1 = "info:fedora/resource1";
        final var resource2 =  resource1 + "/resource2";

        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, true);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertIndexContains("resource1", resource1);
        assertIndexContains("resource1", resource2);

        index.reset();

        assertIndexDoesNotContain(resource1);
        assertIndexDoesNotContain(resource2);

        util.rebuild();

        assertIndexContains("resource1", resource1);
        assertIndexContains("resource1", resource2);
    }

    @Test
    public void rebuildWhenRepoContainsNonArchivalGroupObject() throws Exception {
        final var session1Id = "session1";
        final var resource1 = "info:fedora/resource1";
        final var resource2 =  resource1 + "/resource2";

        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, false);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertIndexContains("resource1", resource1);
        assertIndexContains("resource1_resource2", resource2);

        index.reset();

        assertIndexDoesNotContain(resource1);
        assertIndexDoesNotContain(resource2);

        util.rebuild();

        assertIndexContains("resource1", resource1);
        assertIndexContains("resource1_resource2", resource2);
    }

    private void assertIndexDoesNotContain(final String resourceId) {
        try {
            index.getMapping(resourceId);
            fail(resourceId + " should not exist in index");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            //do nothing - expected
        }
    }

    private void assertIndexContains(final String expectedOcflId, final String resourceId)
            throws FedoraOCFLMappingNotFoundException {
        assertEquals(expectedOcflId, index.getMapping(resourceId).getOcflObjectId());
    }

    private void createResource(final PersistentStorageSession session,
                                final String resourceId, final boolean isArchivalGroup)
            throws PersistentStorageException {
        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)operation).isArchivalGroup()).thenReturn(isArchivalGroup);
        session.persist(operation);
    }

    private void createChildResource(final PersistentStorageSession session,
                                     final String parentId, final String childId)
            throws PersistentStorageException {
        final var operation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(childId);
        when(operation.getType()).thenReturn(CREATE);
        final var bytes = "test".getBytes();
        final var stream = new ByteArrayInputStream(bytes);
        when(operation.getContentSize()).thenReturn((long)bytes.length);
        when(operation.getContentStream()).thenReturn(stream);
        when(operation.getMimeType()).thenReturn("text/plain");
        when(operation.getFilename()).thenReturn("test");
        when(((CreateResourceOperation)operation).getParentId()).thenReturn(parentId);
        session.persist(operation);
    }

}