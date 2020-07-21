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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Purge Persister tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PurgeResourcePersisterTest {

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private PersistentStorageSession storageSession;

    @Mock
    private OcflObjectSession session;

    @Mock
    private ResourceOperation operation;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    private PurgeResourcePersister persister;

    private static String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setup() throws Exception {
        operation = mock(ResourceOperation.class);
        persister = new PurgeResourcePersister(this.index);
        when(psSession.getId()).thenReturn(SESSION_ID);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
    }

    @Test
    public void testPurgeSubPathBinary() throws Exception {
        final String header_string1 = "{\"parent\":\"info:fedora/an-ocfl-object\",\"id\":" +
            "\"info:fedora/an-ocfl-object/some-subpath\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
            "\"interactionModel\":\"http://www.w3.org/ns/ldp#NonRDFSource\",\"createdDate\":" +
            "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
            "\"archivalGroup\":false,\"objectRoot\":false}";
        final InputStream header_stream1 = new ByteArrayInputStream(header_string1.getBytes());
        when(session.read(".fcrepo/some-subpath.json")).thenReturn(header_stream1);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object/some-subpath");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);
        persister.persist(psSession, operation);
        verify(session).delete(".fcrepo/some-subpath.json");
    }

    @Test
    public void testPurgeSubPathRdf() throws Exception {
        final String header_string = "{\"parent\":\"info:fedora/an-ocfl-object\",\"id\":" +
            "\"info:fedora/an-ocfl-object/some-subpath\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
            "\"interactionModel\":\"http://www.w3.org/ns/ldp#BasicContainer\",\"createdDate\":" +
            "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
            "\"archivalGroup\":false,\"objectRoot\":false}";
        final InputStream header_stream = new ByteArrayInputStream(header_string.getBytes());
        when(session.read(".fcrepo/some-subpath.json")).thenReturn(header_stream);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object/some-subpath");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);
        persister.persist(psSession, operation);
        verify(session).delete(".fcrepo/some-subpath.json");
    }

    @Test(expected = PersistentStorageException.class)
    public void testPurgeSubPathDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object/some-subpath");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);
        doThrow(new PersistentStorageException("error"))
            .when(session).delete(".fcrepo/some-subpath.json");
        persister.persist(psSession, operation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testPurgeFullObjectDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenThrow(new FedoraOcflMappingNotFoundException("error"));

        persister.persist(psSession, operation);
        verify(session).delete("some-subpath");
    }

    @Test
    public void testPurgeFullObjectRdf() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);

        persister.persist(psSession, operation);
        verify(session).deleteObject();
    }

    @Test
    public void testPurgeFullObjectBinary() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);

        persister.persist(psSession, operation);
        verify(session).deleteObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotPartOfObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/some-wrong-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(index.getMapping(eq(SESSION_ID), anyString())).thenReturn(mapping);
        persister.persist(psSession, operation);
    }
}
