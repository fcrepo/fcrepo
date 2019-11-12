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
package org.fcrepo.persistence.ocfl;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.fcrepo.persistence.ocfl.OCFLPersistentSessionManager}
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class OCFLPersistentSessionManagerTest {

    @InjectMocks
    private OCFLPersistentSessionManager sessionFactory;

    private PersistentStorageSession readWriteSession;

    private PersistentStorageSession readOnlySession;

    private final String testSessionId = randomUUID().toString();

    private final String testResourcePath = "/" + randomUUID().toString();

    @Mock
    private FedoraResource resource;

    @Mock
    private ResourceOperation mockOperation;

    @Mock
    private FedoraToOCFLObjectIndex index;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private OCFLObjectSessionFactory objectSessionFactory;

    @Before
    public void setUp() {
        readWriteSession = this.sessionFactory.getSession(testSessionId);
        readOnlySession = this.sessionFactory.getReadOnlySession();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNormalSession() throws Exception {
        final String resourceId = "resource1";
        final String ocflObjectId = "ocflObjectId";
        when(mockOperation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(resourceId))).thenReturn(mapping);
        when(mapping.getOcflObjectId()).thenReturn(ocflObjectId);
        readWriteSession.persist(mockOperation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testPersistNoSession() throws Exception {
        readOnlySession.persist(mockOperation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSessionId() {
        this.sessionFactory.getSession(null);
    }

}
