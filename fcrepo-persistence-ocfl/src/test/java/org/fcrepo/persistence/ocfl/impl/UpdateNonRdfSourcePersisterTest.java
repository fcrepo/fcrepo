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

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author whikloj
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateNonRdfSourcePersisterTest {

    @Mock
    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OcflObjectSession session;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    @Mock
    private OcflPersistentStorageSession psSession;

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");

    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String CONTENT_BODY = "this is some example content";

    private UpdateNonRdfSourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setUp() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(ROOT_RESOURCE_ID);
        when(psSession.getId()).thenReturn(SESSION_ID);

        nonRdfSourceOperation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(nonRdfSourceOperation.getUserPrincipal()).thenReturn(USER_PRINCIPAL);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(-1L);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);


        persister = new UpdateNonRdfSourcePersister(index);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
    }

    @Test
    public void testHandle() {
        assertTrue(this.persister.handle(this.nonRdfSourceOperation));
        final RdfSourceOperation badOperation = mock(RdfSourceOperation.class);
        when(badOperation.getType()).thenReturn(UPDATE);
        assertFalse(this.persister.handle(badOperation));
    }


    @Test
    public void testNonRdfExistingResource() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);

        final var headers = newResourceHeaders(ROOT_RESOURCE_ID, RESOURCE_ID, NON_RDF_SOURCE.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);

        when(session.readHeaders(anyString())).thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());

        final var originalCreation = headers.getCreatedDate();
        final var originalModified = headers.getLastModifiedDate();

        persister.persist(psSession, nonRdfSourceOperation);

        // verify user content
        verify(session).writeResource(headersCaptor.capture(), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        // verify resource headers
        final var resultHeaders = headersCaptor.getValue();

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(originalCreation, resultHeaders.getCreatedDate());

        // The relationship between the actual resource last modified date and the
        // client-asserted last modified data is unclear.
        assertTrue(originalModified.equals(resultHeaders.getLastModifiedDate())
                || originalModified.isBefore(resultHeaders.getLastModifiedDate()));
    }

}
