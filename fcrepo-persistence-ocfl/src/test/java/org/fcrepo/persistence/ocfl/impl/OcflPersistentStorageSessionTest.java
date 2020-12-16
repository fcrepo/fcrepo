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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.cache.NoOpCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Test class for {@link OcflPersistentStorageSession}
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class OcflPersistentStorageSessionTest {

    private OcflPersistentStorageSession session;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private ReindexService reindexService;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraOcflMapping mapping2;

    @Mock
    private OcflObjectSessionFactory mockSessionFactory;

    @Mock
    private OcflObjectSession objectSession1;

    @Mock
    private OcflObjectSession objectSession2;

    private DefaultOcflObjectSessionFactory objectSessionFactory;

    private static final FedoraId ROOT_OBJECT_ID = FedoraId.create("info:fedora/resource1");

    private static final FedoraId RESOURCE_ID = ROOT_OBJECT_ID;

    private static final String OCFL_RESOURCE_ID = RESOURCE_ID.getResourceId();

    private static final FedoraId ROOT_OBJECT_ID_2 = FedoraId.create("info:fedora/resource2");

    private static final FedoraId RESOURCE_ID2 = ROOT_OBJECT_ID_2;

    private static final String OCFL_RESOURCE_ID2 = RESOURCE_ID2.getResourceId();

    private static final String USER_PRINCIPAL = "fedoraUser";

    private RdfSourceOperation rdfSourceOperation;

    private RdfSourceOperation rdfSourceOperation2;

    private CreateRdfSourceOperation createArchivalGroupOperation;

    private static final String BINARY_CONTENT = "Some test content";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path stagingDir;

    @Mock
    private ResourceOperation unsupportedOperation;

    @Before
    public void setUp() throws Exception {
        stagingDir = tempFolder.newFolder("ocfl-staging").toPath();
        final var repoDir = tempFolder.newFolder("ocfl-repo").toPath();
        final var workDir = tempFolder.newFolder("ocfl-work").toPath();

        final var objectMapper = OcflPersistentStorageUtils.objectMapper();
        final var repository = createFilesystemRepository(repoDir, workDir);
        objectSessionFactory = new DefaultOcflObjectSessionFactory(repository, stagingDir,
                objectMapper,
                new NoOpCache<>(),
                CommitType.NEW_VERSION,
                "Fedora 6 test", "fedoraAdmin", "info:fedora/fedoraAdmin");
        session = createSession(index, objectSessionFactory);

        // Create rdf operations implement two interfaces
        rdfSourceOperation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        rdfSourceOperation2 = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));

        createArchivalGroupOperation = mock(CreateRdfSourceOperation.class);
        when(createArchivalGroupOperation.isArchivalGroup()).thenReturn(true);
    }

    private OcflPersistentStorageSession createSession(final FedoraToOcflObjectIndex index,
                                                       final OcflObjectSessionFactory objectSessionFactory) {
        final var sessionId = UUID.randomUUID().toString();
        return new OcflPersistentStorageSession(sessionId, index, objectSessionFactory, reindexService);
    }

    private void mockNoIndex(final FedoraId resourceId) throws FedoraOcflMappingNotFoundException {
        when(index.getMapping(any(), eq(resourceId)))
                .thenThrow(new FedoraOcflMappingNotFoundException(resourceId.getFullId()));
    }

    private void mockMappingAndIndexWithNoIndex(final String ocflObjectId, final FedoraId resourceId,
                                                final FedoraId rootObjectId, final FedoraOcflMapping mapping)
            throws FedoraOcflMappingNotFoundException {
        mockMapping(ocflObjectId, rootObjectId, mapping);
        when(index.getMapping(any(), eq(resourceId)))
                .thenThrow(new FedoraOcflMappingNotFoundException(resourceId.getFullId()))
                .thenReturn(mapping);
    }

    private void mockMappingAndIndex(final String ocflObjectId, final FedoraId resourceId, final FedoraId rootObjectId,
                                     final FedoraOcflMapping mapping) throws FedoraOcflMappingNotFoundException {
        mockMapping(ocflObjectId, rootObjectId, mapping);
        when(index.getMapping(any(), eq(resourceId))).thenReturn(mapping);
    }

    private void mockMapping(final String ocflObjectId, final FedoraId rootObjectId, final FedoraOcflMapping mapping) {
        when(mapping.getOcflObjectId()).thenReturn(ocflObjectId);
        when(mapping.getRootObjectIdentifier()).thenReturn(rootObjectId);
    }

    private void mockResourceOperation(final RdfSourceOperation rdfSourceOperation, final RdfStream userStream,
                                       final String userPrincipal, final FedoraId resourceId) {
        when(rdfSourceOperation.getTriples()).thenReturn(userStream);
        when(rdfSourceOperation.getResourceId()).thenReturn(FedoraId.create(resourceId.getFullId()));
        when(((CreateResourceOperation) rdfSourceOperation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(rdfSourceOperation.getType()).thenReturn(CREATE);
        when(rdfSourceOperation.getUserPrincipal()).thenReturn(userPrincipal);
        when(((CreateResourceOperation) rdfSourceOperation).getInteractionModel())
                .thenReturn(BASIC_CONTAINER.toString());
    }

    private void mockResourceOperation(final RdfSourceOperation rdfSourceOperation, final FedoraId resourceId) {
        final Node resourceUri = createURI(resourceId.getFullId());
        mockResourceOperation(rdfSourceOperation, new DefaultRdfStream(resourceUri, Stream.empty()),
                USER_PRINCIPAL,
                resourceId);
    }

    @Test
    public void roundtripCreateContainerCreation() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID.getFullId());

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, RESOURCE_ID);
        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        final var node = createURI(RESOURCE_ID.getFullId());

        //verify get triples within the transaction
        var retrievedUserStream = session.getTriples(RESOURCE_ID, null);
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());

        //verify get server props within the transaction
        final var headers = session.getHeaders(RESOURCE_ID, null);
        assertEquals(USER_PRINCIPAL, headers.getCreatedBy());
        final var originalCreatedDate = headers.getCreatedDate();
        assertNotNull("Headers must contain created date", originalCreatedDate);
        final var originalModifiedDate = headers.getLastModifiedDate();
        assertNotNull("Headers must contain modified date", originalModifiedDate);

        session.prepare();
        //commit to OCFL
        session.commit();

        //create a new session and verify that the state is the same
        final OcflPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //verify get triples outside of the transaction
        retrievedUserStream = newSession.getTriples(RESOURCE_ID, null);
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());

        //verify get server props outside of the transaction
        final var headers2 = newSession.getHeaders(RESOURCE_ID, null);
        assertEquals(originalCreatedDate, headers2.getCreatedDate());
        assertEquals(originalModifiedDate, headers2.getLastModifiedDate());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedPersistOperation() throws Exception {
        this.session.persist(unsupportedOperation);
    }

    @Test
    public void getTriplesOfBinaryDescription() throws Exception {

        final FedoraId descriptionResource = RESOURCE_ID.resolve(FedoraTypes.FCR_METADATA);
        mockMappingAndIndex(OCFL_RESOURCE_ID, descriptionResource, ROOT_OBJECT_ID, mapping);
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID.getFullId());

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL,
                descriptionResource);

        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();
        //commit to OCFL
        session.commit();

        //create a new session and verify the returned rdf stream.
        final OcflPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //verify get triples outside of the transaction
        final RdfStream retrievedUserStream = newSession.getTriples(descriptionResource, null);
        assertEquals(resourceUri, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());
    }

    @Test
    public void persistFailsIfCommitAlreadyComplete() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();
        //commit to OCFL
        session.commit();

        //this should fail
        try {
            session.persist(rdfSourceOperation);
            fail("second session.persist(...) invocation should have failed.");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void getTriplesFailsIfAlreadyCommitted() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();
        //commit to OCFL
        session.commit();

        //this should fail
        try {
            session.getTriples(RESOURCE_ID, null);
            fail("second session.getTriples(...) invocation should have failed.");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void commitFailsIfAlreadyCommitted() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();
        //commit to OCFL
        session.commit();

        //this should fail
        try {
            session.commit();
            fail("second session.commit(...) invocation should have failed.");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void commitFailsIfNotPrepared() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        try {
            session.commit();
            fail("commit should have failed because prepare() was not called");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void prepareFailsIfAlreadyPrepared() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();

        try {
            session.prepare();
            fail("prepare should have failed because prepare() was already called");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void verifyGetTriplesFailsAfterRollback() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //verify that getting triples within transaction succeeds
        session.getTriples(RESOURCE_ID, null);

        //rollback
        session.rollback();

        //get triples should now fail because the session is effectively closed.
        try {
            session.getTriples(RESOURCE_ID, null);
            fail("session.getTriples(...) invocation after rollback should have failed.");
        } catch (final PersistentStorageException ex) {
            //expected failure
        }
    }

    /*
     * This test covers the expected behavior when two OCFL Object Sessions are modified and one of the commits to
     * the mutable head fails.
     */
    @Test(expected = PersistentStorageException.class)
    public void rollbackOnSessionWithCommitsToMutableHeadShouldFail() throws Exception {
        mockNoIndex(RESOURCE_ID);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        mockNoIndex(RESOURCE_ID2);
        mockResourceOperation(rdfSourceOperation2, RESOURCE_ID2);

        //mock success on commit for the first object session
        when(mockSessionFactory.newSession(eq(OCFL_RESOURCE_ID))).thenReturn(objectSession1);
        //mock failure on commit for the second object session
        when(mockSessionFactory.newSession(eq(OCFL_RESOURCE_ID2))).thenReturn(objectSession2);
        doThrow(RuntimeException.class).when(objectSession2).commit();
        doThrow(IllegalStateException.class).when(objectSession1).rollback();

        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        try {
            //perform the create rdf operations
            session1.persist(rdfSourceOperation);
            session1.persist(rdfSourceOperation2);
        } catch (final PersistentStorageException e) {
            fail("Operations should not fail.");
        }

        //get triples should now fail because the session is effectively closed.
        try {
            session1.prepare();
            session1.commit();
            fail("session1.commit(...) invocation should fail.");
        } catch (final PersistentStorageException ex) {
            session1.rollback();
            fail("session1.rollback(...) invocation should fail.");
        }
    }

    @Test
    public void getTriplesFailsIfCommitHasAlreadyStarted() throws Exception {
        final var ocflId = OCFL_RESOURCE_ID;
        mockNoIndex(RESOURCE_ID);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //mock success on commit for the first object session
        when(mockSessionFactory.newSession(eq(ocflId))).thenReturn(objectSession1);
        //pause on commit for 1 second
        doAnswer(invocation -> {
            Thread.sleep(1000);
            return null;
        }).when(objectSession1).commit();

        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        //persist the operation
        try {
            session1.persist(rdfSourceOperation);
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                session1.prepare();
                session1.commit();
            } catch (final PersistentStorageException e) {
                fail("The commit() should not fail.");
            } finally {
                latch.countDown();
            }
        }).start();

        //sleep for a fraction of a second to ensure
        //commit thread runs first.
        Thread.sleep(500);
        //get triples should now fail because the commit has started.
        try {
            session1.getTriples(RESOURCE_ID, null);
            fail("session1.getTriples(...) invocation should have failed.");
        } catch (final PersistentStorageException e) {
            //do nothing
        }

        latch.await(1000, TimeUnit.MILLISECONDS);

        verify(objectSession1).commit();
    }

    @Test
    public void rollbackDoesNotFailWhenAlreadyCommitted() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        final PersistentStorageSession session1 = createSession(index, objectSessionFactory);
        //persist the operation
        try {
            session1.persist(rdfSourceOperation);
            session1.prepare();
            session1.commit();
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        session1.rollback();
    }

    @Test(expected = PersistentStorageException.class)
    public void rollbackSucceedsOnUncommittedChanges() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);
        final PersistentStorageSession session1 = createSession(index, this.objectSessionFactory);

        try {
            session1.persist(rdfSourceOperation);
            session1.rollback();
        } catch (final PersistentStorageException e) {
            fail("Neither persist() nor rollback() should have failed.");

        }
        //verify that the resource cannot be found now (since it wasn't committed).
        final OcflPersistentStorageSession newSession = createSession(index, objectSessionFactory);
        newSession.getTriples(RESOURCE_ID, null);
        fail("second session.getTriples(...) invocation should have failed.");
    }

    @Test(expected = PersistentStorageException.class)
    public void rollbackFailsWhenAlreadyRolledBack() throws Exception {
        mockNoIndex(RESOURCE_ID);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);
        when(mockSessionFactory.newSession(eq(OCFL_RESOURCE_ID))).thenReturn(objectSession1);
        doThrow(new RuntimeException("commit error")).when(objectSession1).commit();
        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        try {
            session1.persist(rdfSourceOperation);
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        try {
            session1.prepare();
            session1.commit();
            fail("Operation should fail.");
        } catch (final PersistentStorageException e) {
            //expected failure
        }

        try {
            session1.rollback();
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        session1.rollback();
    }

    @Test
    public void getTriplesFromPreviousVersion() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID.getFullId());

        //mock the operation
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);

        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);
        session.prepare();
        //commit to new version
        session.commit();


        //create a new session and verify that the state is the same
        final OcflPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //get the newly created version
        final List<Instant> versions = newSession.listVersions(RESOURCE_ID);
        final Instant version = versions.get(versions.size() - 1);

        //verify get triples from version
        final RdfStream retrievedUserStream = newSession.getTriples(RESOURCE_ID, version);
        final var node = createURI(RESOURCE_ID.getFullId());
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());
    }

    @Test
    public void listVersionsOfAResourceContainedInAnArchivalGroup() throws Exception {
        final Node resourceUri = createURI(RESOURCE_ID.getFullId());

        mockMapping(OCFL_RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        final var mappingCount = new AtomicInteger(0);
        when(index.getMapping(anyString(), eq(RESOURCE_ID))).thenAnswer(
                (Answer<FedoraOcflMapping>) invocationOnMock -> {
            final var current = mappingCount.getAndIncrement();
            if (current == 0) {
                throw new FedoraOcflMappingNotFoundException("");
            }
            return mapping;
        });

        mockResourceOperation(createArchivalGroupOperation, RESOURCE_ID);
        session.persist(createArchivalGroupOperation);
        session.prepare();
        session.commit();

        final var childId = RESOURCE_ID.resolve("child");
        when(index.getMapping(anyString(), eq(childId))).thenReturn(mapping);

        final String title = "title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);

        final var session2 = createSession(index, objectSessionFactory);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, childId);
        when(((CreateResourceOperation) rdfSourceOperation).getParentId())
                .thenReturn(RESOURCE_ID);

        session2.persist(rdfSourceOperation);
        session2.prepare();
        session2.commit();

        final var session3 = createSession(index, objectSessionFactory);

        final var agVersions = session3.listVersions(RESOURCE_ID);
        final var childVersions = session3.listVersions(childId);

        assertEquals(2, agVersions.size());
        assertEquals(1, childVersions.size());
        assertThat(agVersions, hasItems(childVersions.get(0)));
    }

    @Test
    public void getBinaryContent() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);
        session.prepare();
        // commit to OCFL
        session.commit();

        // create a new session and verify the returned rdf stream.
        final var newSession = createSession(index, objectSessionFactory);
        final var result = IOUtils.toString(newSession.getBinaryContent(RESOURCE_ID, null), UTF_8);

        assertEquals(BINARY_CONTENT, result);
    }

    @Test
    public void getBinaryContentFailsIfAlreadyCommitted() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);
        session.prepare();
        // commit to OCFL
        session.commit();

        try {
            session.getBinaryContent(RESOURCE_ID, null);
            fail("Get must fail due to session having been committed");
        } catch (final PersistentStorageException ex) {
            // expected failure, handled with catch since the persist can throw same error
        }
    }

    @Test
    public void getBinaryContentVersion() throws Exception {
        // SEE getTriplesFromPreviousVersion
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);
        // perform the create non-rdf source operation
        session.persist(binOperation);
        session.prepare();
        // commit to OCFL
        session.commit();

        // create a new session and verify that the state is the same
        final var newSession = createSession(index, objectSessionFactory);

        final var versions = newSession.listVersions(RESOURCE_ID);
        final var version1 = versions.get(versions.size() - 1);
        assertEquals(BINARY_CONTENT,
                IOUtils.toString(newSession.getBinaryContent(RESOURCE_ID, version1), UTF_8));
    }

    @Test
    public void getBinaryContentUncommitted() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);

        // create a new session and verify the returned rdf stream.
        final var result = IOUtils.toString(session.getBinaryContent(RESOURCE_ID, null), UTF_8);

        assertEquals(BINARY_CONTENT, result);
    }

    @Test
    public void readOnlySessionShouldExpireObjectSessions() throws Exception {
        mockMappingAndIndex(OCFL_RESOURCE_ID, RESOURCE_ID, RESOURCE_ID, mapping);

        // create resource
        final Node resourceUri = createURI(RESOURCE_ID.getFullId());

        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral("my title"));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, RESOURCE_ID);
        session.persist(rdfSourceOperation);
        session.prepare();
        session.commit();

        // read-only read resource
        final var sessionMap = Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .<String, OcflObjectSession>build()
                .asMap();

        final var readOnlySession = new OcflPersistentStorageSession(null, index, objectSessionFactory, reindexService);
        ReflectionTestUtils.setField(readOnlySession, "sessionMap", sessionMap);

        readOnlySession.getHeaders(RESOURCE_ID, null);

        assertTrue(sessionMap.containsKey(RESOURCE_ID.getFullId()));

        TimeUnit.SECONDS.sleep(2);

        assertFalse(sessionMap.containsKey(RESOURCE_ID.getFullId()));
    }

    private NonRdfSourceOperation mockNonRdfSourceOperation(final String content,
            final String userPrincipal, final FedoraId resourceId) {
        final var binOperation = mock(NonRdfSourceOperation.class,
                withSettings().extraInterfaces(CreateResourceOperation.class));

        final var contentStream = new ByteArrayInputStream(content.getBytes());
        when(binOperation.getContentStream()).thenReturn(contentStream);
        when(binOperation.getContentSize()).thenReturn(-1L);
        when(binOperation.getResourceId()).thenReturn(resourceId);
        when(((CreateResourceOperation) binOperation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(binOperation.getType()).thenReturn(CREATE);
        when(binOperation.getUserPrincipal()).thenReturn(userPrincipal);
        when(((CreateResourceOperation) binOperation).getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        return binOperation;
    }
}
