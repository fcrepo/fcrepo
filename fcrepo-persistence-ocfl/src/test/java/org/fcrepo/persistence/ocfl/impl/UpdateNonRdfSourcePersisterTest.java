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

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author whikloj
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateNonRdfSourcePersisterTest {

    @Mock
    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private FedoraToOCFLObjectIndex index;
    @Mock
    private WriteOutcome writeOutcome;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<InputStream> headersIsCaptor;

    @Mock
    private OCFLPersistentStorageSession psSession;

    private static final String RESOURCE_ID = "info:fedora/parent/child";

    private static final String ROOT_RESOURCE_ID = "info:fedora/parent";

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String CONTENT_BODY = "this is some example content";

    private static final Long LOCAL_CONTENT_SIZE = Long.valueOf(CONTENT_BODY.length());

    private UpdateNonRdfSourcePersister persister;

    @Before
    public void setUp() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(ROOT_RESOURCE_ID);

        when(session.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);

        nonRdfSourceOperation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(nonRdfSourceOperation.getUserPrincipal()).thenReturn(USER_PRINCIPAL);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(null);

        when(writeOutcome.getContentSize()).thenReturn(LOCAL_CONTENT_SIZE);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(anyString())).thenReturn(mapping);
        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);


        persister = new UpdateNonRdfSourcePersister(index);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(anyString())).thenReturn(mapping);

    }

    @Test
    public void testHandle(){
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
        final var headerStream = serializeHeaders(headers);
        when(session.read(anyString())).thenReturn(headerStream);

        final var originalCreation = headers.getCreatedDate();
        final var originalModified = headers.getLastModifiedDate();

        persister.persist(psSession, nonRdfSourceOperation);

        // verify user content
        verify(session).write(eq("child"), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(originalCreation, resultHeaders.getCreatedDate());
        assertTrue(originalModified + " is not before " + resultHeaders.getLastModifiedDate(),
                originalModified.isBefore(resultHeaders.getLastModifiedDate()));

        assertEquals(LOCAL_CONTENT_SIZE, resultHeaders.getContentSize());
    }

    private ResourceHeaders retrievePersistedHeaders(final String subpath) throws Exception {
        verify(session).write(eq(getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION),
                headersIsCaptor.capture());
        final var headersIs = headersIsCaptor.getValue();
        return deserializeHeaders(headersIs);
    }
}
