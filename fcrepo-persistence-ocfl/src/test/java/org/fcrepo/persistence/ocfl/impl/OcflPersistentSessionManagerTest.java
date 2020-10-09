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

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Test class for {@link OcflPersistentSessionManager}
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class OcflPersistentSessionManagerTest {

    private OcflPersistentSessionManager sessionManager;

    private PersistentStorageSession readWriteSession;

    private PersistentStorageSession readOnlySession;

    private final String testSessionId = randomUUID().toString();

    @Mock
    private ResourceOperation mockOperation;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflObjectSessionFactory objectSessionFactory;

    @Before
    public void setUp() throws IOException {
        this.sessionManager = new OcflPersistentSessionManager();
        readWriteSession = this.sessionManager.getSession(testSessionId);
        setField(sessionManager, "objectSessionFactory", objectSessionFactory);
        setField(sessionManager, "ocflIndex", index);
        readOnlySession = this.sessionManager.getReadOnlySession();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedOperationOnUnrecognizedOperation() throws Exception {
        readWriteSession.persist(mockOperation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testPersistNoSession() throws Exception {
        readOnlySession.persist(mockOperation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSessionId() {
        this.sessionManager.getSession(null);
    }

    @Test
    public void removeSession() {
        final var session = sessionManager.removeSession(testSessionId);
        assertSame(readWriteSession, session);
        assertNull(sessionManager.removeSession(testSessionId));
    }

}
