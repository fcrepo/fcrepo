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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.identifiers.FedoraId;
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
 * Delete Persister tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteResourcePersisterTest {

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

    private DeleteResourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setup() throws Exception {
        operation = mock(ResourceOperation.class);
        persister = new DeleteResourcePersister(this.index);
        when(psSession.getId()).thenReturn(SESSION_ID);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
    }

    @Test
    public void testDeleteSubPathBinary() throws Exception {
        final String header_string1 = "{\"parent\":\"info:fedora/an-ocfl-object\",\"id\":" +
            "\"info:fedora/an-ocfl-object/some-subpath\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
            "\"interactionModel\":\"http://www.w3.org/ns/ldp#NonRDFSource\",\"createdDate\":" +
            "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
            "\"archivalGroup\":false,\"objectRoot\":false,\"contentPath\":\"some-subpath\"}";
        final InputStream header_stream1 = new ByteArrayInputStream(header_string1.getBytes());
        when(session.read(".fcrepo/some-subpath.json")).thenReturn(header_stream1);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object/some-subpath"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        persister.persist(psSession, operation);
        verify(session).delete("some-subpath");
        verify(session).read(".fcrepo/some-subpath.json");
        verify(session).write(eq(".fcrepo/some-subpath.json"), any());
    }

    @Test
    public void testDeleteSubPathRdf() throws Exception {
        final String header_string = "{\"parent\":\"info:fedora/an-ocfl-object\",\"id\":" +
            "\"info:fedora/an-ocfl-object/some-subpath\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
            "\"interactionModel\":\"http://www.w3.org/ns/ldp#BasicContainer\",\"createdDate\":" +
            "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
            "\"archivalGroup\":false,\"objectRoot\":false,\"contentPath\":\"some-subpath.nt\"}";
        final InputStream header_stream = new ByteArrayInputStream(header_string.getBytes());
        when(session.read(".fcrepo/some-subpath.json")).thenReturn(header_stream);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object/some-subpath"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        persister.persist(psSession, operation);
        verify(session).delete("some-subpath.nt");
        verify(session).read(".fcrepo/some-subpath.json");
        verify(session).write(eq(".fcrepo/some-subpath.json"), any());
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteSubPathDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object/some-subpath"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(session.read(".fcrepo/some-subpath.json")).thenThrow(
                new PersistentStorageException("error")
        );
        persister.persist(psSession, operation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteFullObjectDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(index.getMapping(eq(SESSION_ID), any())).thenThrow(new FedoraOcflMappingNotFoundException("error"));

        persister.persist(psSession, operation);
        verify(session).delete("some-subpath");
    }


    @Test
    public void testDeleteFullObjectRdf() throws Exception {
        final String header_string = "{\"parent\":\"info:fedora\",\"id\":" +
            "\"info:fedora/an-ocfl-object\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
            "\"interactionModel\":\"http://www.w3.org/ns/ldp#BasicContainer\",\"createdDate\":" +
            "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
            "\"archivalGroup\":false,\"objectRoot\":true,\"contentPath\":\"some-ocfl-id.nt\"}";
        final InputStream rdf_header_stream = new ByteArrayInputStream(header_string.getBytes());
        when(session.listHeadSubpaths()).thenReturn(Stream.of("some-ocfl-id.nt", ".fcrepo/some-ocfl-id.json"));
        when(session.read(".fcrepo/some-ocfl-id.json")).thenReturn(rdf_header_stream);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);

        persister.persist(psSession, operation);
        verify(session).delete("some-ocfl-id.nt");
        verify(session).read(".fcrepo/some-ocfl-id.json");
        verify(session).write(eq(".fcrepo/some-ocfl-id.json"), any());
    }

    @Test
    public void testDeleteFullObjectBinary() throws Exception {
        final String header_string1 = "{\"parent\":\"info:fedora\",\"id\":" +
                "\"info:fedora/an-ocfl-object\",\"lastModifiedDate\":\"2020-04-14T03:42:00.765231Z\"," +
                "\"interactionModel\":\"http://www.w3.org/ns/ldp#NonRDFSource\",\"createdDate\":" +
                "\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
                "\"archivalGroup\":false,\"objectRoot\":true,\"contentPath\":\"some-ocfl-id\"}";
        final InputStream header_stream1 = new ByteArrayInputStream(header_string1.getBytes());
        final String header_string2 = "{\"parent\":\"info:fedora/an-ocfl-object/sub-path\",\"id\":" +
                "\"info:fedora/an-ocfl-object/some-subpath-desc\",\"lastModifiedDate\":" +
                "\"2020-04-14T03:42:00.765231Z\",\"interactionModel\":" +
                "\"http://fedora.info/definitions/v4/repository#NonRdfSourceDescription\"," +
                "\"createdDate\":\"2020-04-14T03:42:00.765231Z\",\"stateToken\":\"6763672ED325A4B632B450545518B34B\"," +
                "\"archivalGroup\":false,\"objectRoot\":false,\"contentPath\":\"some-ocfl-id-description.nt\"}";
        final InputStream header_stream2 = new ByteArrayInputStream(header_string2.getBytes());
        when(session.listHeadSubpaths()).thenReturn(Stream.of("some-ocfl-id", ".fcrepo/some-ocfl-id.json",
                "some-ocfl-id-description.nt", ".fcrepo/some-ocfl-id-description.json"));
        when(session.read(".fcrepo/some-ocfl-id.json")).thenReturn(header_stream1);
        when(session.read(".fcrepo/some-ocfl-id-description.json")).thenReturn(header_stream2);
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);

        persister.persist(psSession, operation);
        verify(session).delete("some-ocfl-id");
        verify(session).read(".fcrepo/some-ocfl-id.json");
        verify(session).write(eq(".fcrepo/some-ocfl-id.json"), any());
        verify(session).delete("some-ocfl-id-description.nt");
        verify(session).read(".fcrepo/some-ocfl-id-description.json");
        verify(session).write(eq(".fcrepo/some-ocfl-id-description.json"), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotPartOfObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/some-wrong-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        persister.persist(psSession, operation);
    }
}
