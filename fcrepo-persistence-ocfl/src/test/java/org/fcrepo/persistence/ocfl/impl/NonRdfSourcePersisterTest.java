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
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
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
public class NonRdfSourcePersisterTest {

    @Mock
    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private WriteOutcome writeOutcome;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<InputStream> headersIsCaptor;

    private static final String RESOURCE_ID = "info:fedora/parent/child";

    private static final String PARENT_RESOURCE_ID = "info:fedora/parent";

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String CONTENT_BODY = "this is some example content";

    private static final Long LOCAL_CONTENT_SIZE = Long.valueOf(CONTENT_BODY.length());

    private static final String EXTERNAL_URL = "http://example.com/file.txt";

    private static final String EXTERNAL_HANDLING = "proxy";

    private static final Long EXTERNAL_CONTENT_SIZE = 526632l;

    private final NonRdfSourcePersister persister = new NonRdfSourcePersister();

    @Before
    public void setUp() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(PARENT_RESOURCE_ID);

        when(session.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);

        nonRdfSourceOperation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(nonRdfSourceOperation.getUserPrincipal()).thenReturn(USER_PRINCIPAL);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(null);

        when(writeOutcome.getContentSize()).thenReturn(LOCAL_CONTENT_SIZE);
    }

    @Test
    public void testNonRdfNewResource() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(session, nonRdfSourceOperation, mapping);

        // verify user content
        verify(session).write(eq("child"), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(LOCAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
    }

    @Test
    public void testNonRdfNewExternalBinary() throws Exception {

        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        when(nonRdfSourceOperation.getContentUri()).thenReturn(URI.create(EXTERNAL_URL));
        when(nonRdfSourceOperation.getExternalHandling()).thenReturn(EXTERNAL_HANDLING);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(EXTERNAL_CONTENT_SIZE);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel()).thenReturn(NON_RDF_SOURCE
                .toString());

        persister.persist(session, nonRdfSourceOperation, mapping);

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(EXTERNAL_HANDLING, resultHeaders.getExternalHandling());
        assertEquals(EXTERNAL_URL, resultHeaders.getExternalUrl());
        assertEquals(EXTERNAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
    }

    @Test
    public void testNonRdfExistingResource() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);

        final var headers = newResourceHeaders(PARENT_RESOURCE_ID, RESOURCE_ID, NON_RDF_SOURCE.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);
        final var headerStream = serializeHeaders(headers);
        when(session.read(anyString())).thenReturn(headerStream);

        final var originalCreation = headers.getCreatedDate();
        final var originalModified = headers.getLastModifiedDate();

        persister.persist(session, nonRdfSourceOperation, mapping);

        // verify user content
        verify(session).write(eq("child"), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(originalCreation, resultHeaders.getCreatedDate());
        assertTrue(originalModified.isBefore(resultHeaders.getLastModifiedDate()));

        assertEquals(LOCAL_CONTENT_SIZE, resultHeaders.getContentSize());
    }

    @Test(expected = PersistentStorageException.class)
    public void testNonRdfContentSizeMismatch() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(99l);
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(session, nonRdfSourceOperation, mapping);
    }

    private ResourceHeaders retrievePersistedHeaders(final String subpath) throws Exception {
        verify(session).write(eq(getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION),
                headersIsCaptor.capture());
        final var headersIs = headersIsCaptor.getValue();
        return deserializeHeaders(headersIs);
    }

    private void assertModificationHeadersSet(final ResourceHeaders headers) {
        assertEquals(USER_PRINCIPAL, headers.getCreatedBy());
        assertNotNull(headers.getCreatedDate());
        assertEquals(USER_PRINCIPAL, headers.getLastModifiedBy());
        assertNotNull(headers.getLastModifiedDate());
    }
}
