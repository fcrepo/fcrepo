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

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.time.Instant;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OCFLPersistentSessionFactoryTest {

    private PersistentStorageSessionFactory sessionFactory;

    private PersistentStorageSession readWriteSession;

    private PersistentStorageSession readOnlySession;

    private final String testTxId = randomUUID().toString();

    private final String testResourcePath = "/" + randomUUID().toString();

    @Mock
    private FedoraResource resource;

    @Before
    public void setUp() {
        this.sessionFactory = new OCFLPersistentSessionFactory();
        readWriteSession = this.sessionFactory.getSession(testTxId);
        readOnlySession = this.sessionFactory.getReadOnlySession();
    }

    @Test
    public void testNormalSession() throws Exception {

        final FedoraResource response1 = readWriteSession.create(resource);
        assertNotNull(response1);

        final FedoraResource response2 = readWriteSession.update(resource);
        assertNotNull(response2);

        final FedoraResource response3 = readWriteSession.delete(resource);
        assertNull(response3);

        final FedoraResource response4 = readWriteSession.read(testResourcePath);
        assertNull(response4);

    }

    @Test
    public void testReadVersionInSession() throws Exception {
        final Instant version = Instant.now();
        final FedoraResource response = readWriteSession.read(testResourcePath, version);
        assertNull(response);
    }

    @Test(expected = PersistentStorageException.class)
    public void testWriteNoSession() throws Exception {
        readOnlySession.create(resource);
    }

    @Test(expected = PersistentStorageException.class)
    public void testUpdateNoSession() throws Exception {
        readOnlySession.update(resource);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteNoSession() throws Exception {
        readOnlySession.delete(resource);
    }

}
