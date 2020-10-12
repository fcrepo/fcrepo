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

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ResourceFactoryImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static String LAST_MODIFIED_BY = "user2";

    private final static String STATE_TOKEN = "stately_value";

    private final static long CONTENT_SIZE = 100L;

    private final static String MIME_TYPE = "text/plain";

    private final static String FILENAME = "testfile.txt";

    private final static URI DIGEST = URI.create("sha:12345");

    private final static Collection<URI> DIGESTS = asList(DIGEST);

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession psSession;

    private ResourceHeadersImpl resourceHeaders;

    private String fedoraIdStr;

    private String sessionId;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId fedoraId;

    private String fedoraMementoIdStr;

    @Mock
    private Transaction mockTx;

    @Mock
    private FedoraResource mockResource;

    @Inject
    private ContainmentIndex containmentIndex;

    @InjectMocks
    private ResourceFactoryImpl factory;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        fedoraIdStr = FEDORA_ID_PREFIX + "/" + UUID.randomUUID().toString();
        fedoraId = FedoraId.create(fedoraIdStr);
        fedoraMementoIdStr = fedoraIdStr + "/fcr:versions/20000102120000";

        sessionId = UUID.randomUUID().toString();
        when(mockTx.getId()).thenReturn(sessionId);

        factory = new ResourceFactoryImpl();

        setField(factory, "persistentStorageSessionManager", sessionManager);
        setField(factory, "containmentIndex", containmentIndex);

        resourceHeaders = new ResourceHeadersImpl();
        resourceHeaders.setId(fedoraId);

        when(sessionManager.getSession(sessionId)).thenReturn(psSession);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);

        when(psSession.getHeaders(eq(fedoraId), nullable(Instant.class))).thenReturn(resourceHeaders);
    }

    @After
    public void cleanUp() {
        when(mockResource.getFedoraId()).thenReturn(rootId);
        containmentIndex.rollbackTransaction(mockTx.getId());
        containmentIndex.getContains(null, rootId).forEach(c ->
                containmentIndex.removeContainedBy(mockTx.getId(), rootId, FedoraId.create(c)));
        containmentIndex.commitTransaction(mockTx.getId());
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
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getReadOnlySession();
    }

    @Test
    public void getResource_BasicContainer_WithParent() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var parentId = FedoraId.create(FEDORA_ID_PREFIX + UUID.randomUUID().toString());
        resourceHeaders.setParent(parentId);

        final var parentHeaders = new ResourceHeadersImpl();
        parentHeaders.setId(parentId);
        populateHeaders(parentHeaders, DIRECT_CONTAINER);

        when(psSession.getHeaders(parentId, null)).thenReturn(parentHeaders);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        final var parentResc = resc.getParent();
        assertTrue("Parent must be a container", parentResc instanceof Container);
        assertEquals(parentId, parentResc.getFedoraId());
        assertStateFieldsMatches(parentResc);
    }

    @Test
    public void getResource_BasicContainer_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(sessionId);
    }

    @Test
    public void getResource_BasicContainer_Cast() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(fedoraId, Container.class);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getReadOnlySession();
    }

    @Test
    public void getResource_BasicContainer_Cast_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId, Container.class);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(sessionId);
    }

    @Test
    public void getResource_DirectContainer() throws Exception {
        populateHeaders(resourceHeaders, DIRECT_CONTAINER);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_IndirectContainer() throws Exception {
        populateHeaders(resourceHeaders, INDIRECT_CONTAINER);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Container);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_Binary() throws Exception {
        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a binary", resc instanceof Binary);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
        assertBinaryFieldsMatch(resc);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void getResource_Binary_StorageException() throws Exception {
        when(psSession.getHeaders(fedoraId, null)).thenThrow(new PersistentStorageException("Boom"));

        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);

        factory.getResource(fedoraId);
    }

    @Test
    public void getResource_ExternalBinary() throws Exception {
        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);
        final String externalUrl = "http://example.com/stuff";
        resourceHeaders.setExternalUrl(externalUrl);
        resourceHeaders.setExternalHandling(PROXY);

        final var resc = factory.getResource(fedoraId);

        assertTrue("Factory must return a container", resc instanceof Binary);
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
        assertBinaryFieldsMatch(resc);
        final var binary = (Binary) resc;
        assertEquals(externalUrl, binary.getExternalURL());
        assertTrue(binary.isProxy());
    }

    @Test
    public void getNonRdfSourceDescription() throws Exception {
        final FedoraId descFedoraId = FedoraId.create(FEDORA_ID_PREFIX + "/object1/fcr:metadata");

        final var headers = new ResourceHeadersImpl();
        headers.setId(descFedoraId);
        populateHeaders(headers, ResourceFactory.createResource(FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI));
        when(psSession.getHeaders(descFedoraId, null)).thenReturn(headers);

        final var resc = factory.getResource(descFedoraId);
        assertTrue("Factory must return a NonRdfSourceDescripton", resc instanceof NonRdfSourceDescriptionImpl);
    }

    @Test
    public void getChildren_NoChildren() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var childrenStream = factory.getChildren(sessionId, fedoraId);

        assertEquals(0, childrenStream.count());
    }

    @Test
    public void getChildren_DoesNotExist() throws Exception {
        final var childrenStream = factory.getChildren(sessionId, fedoraId);
        assertEquals(0, childrenStream.count());
    }

    @Test
    public void getChildren_WithChildren() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var child1Id = FedoraId.create(UUID.randomUUID().toString());
        final var child1Headers = new ResourceHeadersImpl();
        child1Headers.setId(child1Id);
        populateHeaders(child1Headers, BASIC_CONTAINER);
        when(psSession.getHeaders(child1Id, null)).thenReturn(child1Headers);

        final var childNestedId = FedoraId.create(UUID.randomUUID().toString());
        final var childNestedHeaders = new ResourceHeadersImpl();
        childNestedHeaders.setId(childNestedId);
        populateHeaders(childNestedHeaders, BASIC_CONTAINER);
        when(psSession.getHeaders(childNestedId, null)).thenReturn(childNestedHeaders);

        final var child2Id = FedoraId.create(UUID.randomUUID().toString());
        final var child2Headers = new ResourceHeadersImpl();
        child2Headers.setId(child2Id);
        populateHeaders(child2Headers, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(child2Headers);
        when(psSession.getHeaders(child2Id, null)).thenReturn(child2Headers);

        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        containmentIndex.addContainedBy(mockTx.getId(), fedoraId, child1Id);
        containmentIndex.addContainedBy(mockTx.getId(), child1Id, childNestedId);
        containmentIndex.addContainedBy(mockTx.getId(), fedoraId, child2Id);
        containmentIndex.commitTransaction(mockTx.getId());

        final var childrenStream = factory.getChildren(sessionId, fedoraId);
        final var childrenList = childrenStream.collect(Collectors.toList());

        assertEquals(2, childrenList.size());

        final var child1 = childrenList.stream().filter(c -> c.getFedoraId().equals(child1Id)).findFirst();
        assertTrue(child1.isPresent());
        assertTrue(child1.get() instanceof Container);

        final var child2 = childrenList.stream().filter(c -> c.getFedoraId().equals(child2Id)).findFirst();
        assertTrue(child2.isPresent());
        assertTrue(child2.get() instanceof Binary);
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

    private void assertBinaryFieldsMatch(final FedoraResource resc) {
        final Binary binary = (Binary) resc;
        assertEquals(CONTENT_SIZE, binary.getContentSize());
        assertEquals(MIME_TYPE, binary.getMimeType());
        assertEquals(DIGEST, binary.getContentDigest());
        assertEquals(FILENAME, binary.getFilename());
    }

    private static void populateInternalBinaryHeaders(final ResourceHeadersImpl headers) {
        headers.setContentSize(CONTENT_SIZE);
        headers.setMimeType(MIME_TYPE);
        headers.setDigests(DIGESTS);
        headers.setFilename(FILENAME);
    }
}
