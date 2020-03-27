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
package org.fcrepo.kernel.impl.services;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.fcrepo.kernel.api.models.ExternalContent.COPY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraID;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.UpdateNonRdfSourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class ReplaceBinariesServiceImplTest {

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String FEDORA_ID = "info:fedora/resource1";

    private static final String TX_ID = "tx-1234";

    private final String MIME_TYPE = "text/plain";

    private final String FILENAME = "someFile.txt";

    private final Long FILESIZE = 123l;

    private final Collection<URI> DIGESTS = asList(URI.create("urn:sha1:1234abcd"), URI.create("urn:md5:zyxw9876"));

    @Mock
    private Transaction tx;

    @Mock
    private PersistentStorageSession pSession;

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private ExternalContent externalContent;

    private NonRdfSourceOperationFactory factory;

    private FedoraID fedoraId = FedoraID.create(FEDORA_ID);

    @InjectMocks
    private ReplaceBinariesServiceImpl service;

    @Captor
    private ArgumentCaptor<UpdateNonRdfSourceOperation> operationCaptor;

    @Before
    public void setup() {
        factory = new NonRdfSourceOperationFactoryImpl();
        setField(service, "factory", factory);
        when(psManager.getSession(anyString())).thenReturn(pSession);
    }

    @Test
    public void replaceInternalBinary() throws Exception {
        final String contentString = "This is some test data";
        final var stream = toInputStream(contentString, UTF_8);

        service.perform(TX_ID, USER_PRINCIPAL, fedoraId, FILENAME, MIME_TYPE, DIGESTS, stream, FILESIZE,
                null);
        verify(pSession).persist(operationCaptor.capture());
        final NonRdfSourceOperation op = operationCaptor.getValue();

        assertEquals(FEDORA_ID, operationCaptor.getValue().getResourceId());
        assertEquals(contentString, IOUtils.toString(op.getContentStream(), UTF_8));
        assertPropertiesPopulated(op);
    }

    @Test
    public void replaceExternalBinary() throws Exception {
        final URI uri = URI.create("http://example.org/test/location");
        when(externalContent.getURI()).thenReturn(uri);
        when(externalContent.getHandling()).thenReturn(COPY);

        service.perform(TX_ID, USER_PRINCIPAL, fedoraId, FILENAME, MIME_TYPE, DIGESTS, null, FILESIZE,
                externalContent);
        verify(pSession).persist(operationCaptor.capture());
        final NonRdfSourceOperation op = operationCaptor.getValue();

        assertEquals(FEDORA_ID, operationCaptor.getValue().getResourceId());
        assertEquals(uri, op.getContentUri());
        assertEquals(COPY, op.getExternalHandling());
        assertPropertiesPopulated(op);

        assertNull(op.getContentStream());
    }

    // Check that the content type from the external content link is given preference
    @Test
    public void replaceExternalBinary_WithExternalContentType() throws Exception {
        final URI uri = URI.create("http://example.org/test/location");
        when(externalContent.getURI()).thenReturn(uri);
        when(externalContent.getHandling()).thenReturn(COPY);
        when(externalContent.getContentType()).thenReturn(MIME_TYPE);

        service.perform(TX_ID, USER_PRINCIPAL, fedoraId, FILENAME, "application/octet-stream",
                DIGESTS, null, FILESIZE, externalContent);
        verify(pSession).persist(operationCaptor.capture());
        final NonRdfSourceOperation op = operationCaptor.getValue();

        assertEquals(FEDORA_ID, operationCaptor.getValue().getResourceId());
        assertEquals(uri, op.getContentUri());
        assertEquals(COPY, op.getExternalHandling());
        assertPropertiesPopulated(op);

        assertNull(op.getContentStream());
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void replaceBinary_PersistFailure() throws Exception {
        doThrow(new PersistentStorageException("Boom")).when(pSession)
                .persist(any(ResourceOperation.class));

        final var stream = toInputStream("Some content", UTF_8);

        service.perform(TX_ID, USER_PRINCIPAL, fedoraId, FILENAME, MIME_TYPE, DIGESTS, stream, FILESIZE,
                null);
    }

    private void assertPropertiesPopulated(final NonRdfSourceOperation op) {
        assertEquals(MIME_TYPE, op.getMimeType());
        assertEquals(FILENAME, op.getFilename());
        assertEquals(FILESIZE, op.getContentSize());
        assertEquals(DIGESTS, op.getContentDigests());
    }
}
