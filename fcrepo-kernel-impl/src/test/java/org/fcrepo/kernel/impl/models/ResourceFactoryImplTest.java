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
package org.fcrepo.kernel.impl.models;

import static java.util.Arrays.stream;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author bbpennel
 *
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ResourceFactoryImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static String LAST_MODIFIED_BY = "user2";

    private final static String STATE_TOKEN = "stately_value";

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession psSession;

    private ResourceHeadersImpl resourceHeaders;

    private ResourceFactoryImpl factory;

    private String fedoraId;

    private String sessionId;

    @Mock
    private Transaction mockTx;

    @Before
    public void setup() throws Exception {
        fedoraId = UUID.randomUUID().toString();

        sessionId = UUID.randomUUID().toString();
        when(mockTx.getId()).thenReturn(sessionId);

        factory = new ResourceFactoryImpl();

        setField(factory, "persistentStorageSessionManager", sessionManager);

        resourceHeaders = new ResourceHeadersImpl();
        resourceHeaders.setId(fedoraId);

        when(sessionManager.getSession(sessionId)).thenReturn(psSession);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);

        when(psSession.getHeaders(eq(fedoraId), nullable(Instant.class))).thenReturn(resourceHeaders);
    }

    @Test(expected = PathNotFoundException.class)
    public void getResource_ObjectNotFound() throws Exception {
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);

        factory.getResource(fedoraId);
    }

    @Test(expected = ResourceTypeException.class)
    public void getResource_NoInteractionModel() throws Exception {
        resourceHeaders.setInteractionModel(null);

        factory.getResource(fedoraId);
    }

    @Test(expected = ResourceTypeException.class)
    public void getResource_UnknownInteractionModel() throws Exception {
        resourceHeaders.setInteractionModel("http://example.com/mystery_stroop");

        factory.getResource(fedoraId);
    }

    @Test
    public void getResource_BasicContainer() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getReadOnlySession();
    }

    @Test
    public void getResource_BasicContainer_WithParent() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var parentId = UUID.randomUUID().toString();
        resourceHeaders.setParent(parentId);

        final var parentHeaders = new ResourceHeadersImpl();
        parentHeaders.setId(parentId);
        populateHeaders(parentHeaders, DIRECT_CONTAINER);

        when(psSession.getHeaders(parentId, null)).thenReturn(parentHeaders);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);

        final var parentResc = resc.getParent();
        assertTrue("Parent must be a container", parentResc instanceof Container);
        assertEquals(parentId, parentResc.getId());
        assertStateFieldsMatches(parentResc);
    }

    @Test
    public void getResource_BasicContainer_WithTypes() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);
        resourceHeaders.setTypes(typesToStringList(BASIC_CONTAINER, FEDORA_CONTAINER));

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);
        assertTypesMatch(resc, BASIC_CONTAINER, FEDORA_CONTAINER);
    }

    @Test
    public void getResource_BasicContainer_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(sessionId);
    }

    @Test
    public void getResource_BasicContainer_Cast() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(fedoraId, Container.class);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getReadOnlySession();
    }

    @Test
    public void getResource_BasicContainer_Cast_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId, Container.class);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(sessionId);
    }

    @Test
    public void getResource_DirectContainer() throws Exception {
        populateHeaders(resourceHeaders, DIRECT_CONTAINER);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_IndirectContainer() throws Exception {
        populateHeaders(resourceHeaders, INDIRECT_CONTAINER);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_Binary() throws Exception {
        populateHeaders(resourceHeaders, NON_RDF_SOURCE);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Binary);
        assertEquals(fedoraId, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void getResource_Binary_StorageException() throws Exception {
        when(psSession.getHeaders(fedoraId, null)).thenThrow(new PersistentStorageException("Boom"));

        populateHeaders(resourceHeaders, NON_RDF_SOURCE);

        factory.getResource(fedoraId);
    }

    private void assertTypesMatch(final FedoraResource resc, final Resource... types) {
        assertEquals("Incorrect number of types found", types.length, resc.getTypes().size());
        assertTrue("One or more expected types were missing",
                resc.getTypes().containsAll(typesToUriList(types)));
    }

    private void assertStateFieldsMatches(final FedoraResource resc) {
        assertEquals(CREATED_DATE, resc.getCreatedDate());
        assertEquals(CREATED_BY, resc.getCreatedBy());
        assertEquals(LAST_MODIFIED_DATE, resc.getLastModifiedDate());
        assertEquals(LAST_MODIFIED_BY, resc.getLastModifiedBy());
        assertEquals(STATE_TOKEN, resc.getStateToken());
        assertEquals(STATE_TOKEN, resc.getEtagValue());
    }

    private static void populateHeaders(final ResourceHeadersImpl headers, final Resource ixModel) {
        headers.setInteractionModel(ixModel.getURI());
        headers.setCreatedBy(CREATED_BY);
        headers.setCreatedDate(CREATED_DATE);
        headers.setLastModifiedBy(LAST_MODIFIED_BY);
        headers.setLastModifiedDate(LAST_MODIFIED_DATE);
        headers.setStateToken(STATE_TOKEN);
    }

    private static List<String> typesToStringList(final Resource... types) {
        return stream(types).map(Resource::getURI).collect(Collectors.toList());
    }

    private static List<URI> typesToUriList(final Resource... types) {
        return stream(types).map(Resource::getURI).map(URI::create).collect(Collectors.toList());
    }
}
