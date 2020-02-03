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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.api.CommitOption.UNVERSIONED;

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;

import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.mintOCFLObjectId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Test class for {@link OCFLPersistentStorageSession}
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class OCFLPersistentStorageSessionTest {

    private OCFLPersistentStorageSession session;

    @Mock
    private FedoraToOCFLObjectIndex index;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private FedoraOCFLMapping mapping2;

    @Mock
    private WriteOutcome writeOutcome;

    @Mock
    private OCFLObjectSessionFactory mockSessionFactory;

    @Mock
    private OCFLObjectSession objectSession1;

    @Mock
    private OCFLObjectSession objectSession2;

    private OCFLObjectSessionFactory objectSessionFactory;

    private static final String ROOT_OBJECT_ID = "info:fedora/resource1";

    private static final String RESOURCE_ID = ROOT_OBJECT_ID;

    private static final String ROOT_OBJECT_ID_2 = "info:fedora/resource2";

    private static final String RESOURCE_ID2 = ROOT_OBJECT_ID_2;

    private static final String USER_PRINCIPAL = "fedoraUser";

    private RdfSourceOperation rdfSourceOperation;

    private RdfSourceOperation rdfSourceOperation2;

    private static final String BINARY_CONTENT = "Some test content";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private ResourceOperation unsupportedOperation;

    @Before
    public void setUp() throws Exception {
        final var stagingDir = tempFolder.newFolder("ocfl-staging");
        final var repoDir = tempFolder.newFolder("ocfl-repo");
        final var workDir = tempFolder.newFolder("ocfl-work");

        this.objectSessionFactory = new DefaultOCFLObjectSessionFactory(stagingDir, repoDir, workDir);
        session = createSession(index, objectSessionFactory);
        DefaultOCFLObjectSession.setGlobaDefaultCommitOption(UNVERSIONED);

        // Create rdf operations implement two interfaces
        rdfSourceOperation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        rdfSourceOperation2 = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));

        when(objectSession1.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);
        when(objectSession2.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);
    }

    private OCFLPersistentStorageSession createSession(final FedoraToOCFLObjectIndex index,
                                                       final OCFLObjectSessionFactory objectSessionFactory) {
        return new OCFLPersistentStorageSession(new Random().nextLong() + "", index, objectSessionFactory);
    }

    private void mockMappingAndIndex(final String ocflObjectId, final String resourceId, final String rootObjectId,
                                     final FedoraOCFLMapping mapping) throws FedoraOCFLMappingNotFoundException {
        when(mapping.getOcflObjectId()).thenReturn(ocflObjectId);
        when(mapping.getRootObjectIdentifier()).thenReturn(rootObjectId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

    }

    private void mockResourceOperation(final RdfSourceOperation rdfSourceOperation, final RdfStream userStream,
                                       final String userPrincipal, final String resourceId) {
        when(rdfSourceOperation.getTriples()).thenReturn(userStream);
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);
        when(rdfSourceOperation.getUserPrincipal()).thenReturn(userPrincipal);
    }

    private void mockResourceOperation(final RdfSourceOperation rdfSourceOperation, final String resourceId) {
        final Node resourceUri = createURI(resourceId);
        mockResourceOperation(rdfSourceOperation, new DefaultRdfStream(resourceUri, Stream.empty()),
                USER_PRINCIPAL,
                resourceUri.getURI());
    }

    @Test
    public void roundtripCreateContainerCreation() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID);

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, RESOURCE_ID);
        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        final var node = createURI(RESOURCE_ID);

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

        //commit to OCFL
        session.commit();

        //create a new session and verify that the state is the same
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);

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

        final String descriptionResource = RESOURCE_ID + "/" + FedoraTypes.FCR_METADATA;
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), descriptionResource, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID);

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL,
                descriptionResource);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to OCFL
        session.commit();

        //create a new session and verify the returned rdf stream.
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //verify get triples outside of the transaction
        final RdfStream retrievedUserStream = newSession.getTriples(descriptionResource, null);
        assertEquals(resourceUri, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());
    }

    @Test
    public void persistFailsIfCommitAlreadyComplete() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

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
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

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
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

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
    public void verifyGetTriplesFailsAfterRollback() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
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
        final var ocflId1 = mintOCFLObjectId(RESOURCE_ID);
        final var ocflId2 = mintOCFLObjectId(RESOURCE_ID2);
        mockMappingAndIndex(ocflId1, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        mockMappingAndIndex(ocflId2, RESOURCE_ID2, ROOT_OBJECT_ID_2, mapping2);
        mockResourceOperation(rdfSourceOperation2, RESOURCE_ID2);

        //mock success on commit for the first object session
        when(mockSessionFactory.create(eq(ocflId1), anyString())).thenReturn(objectSession1);
        when(objectSession1.commit(eq(UNVERSIONED))).thenReturn(Instant.now().toString());
        mockOCFLObjectSession(objectSession1, UNVERSIONED);
        //mock failure on commit for the second object session
        when(mockSessionFactory.create(eq(ocflId2), anyString())).thenReturn(objectSession2);
        when(objectSession2.commit(eq(UNVERSIONED))).thenThrow(PersistentStorageException.class);
        mockOCFLObjectSession(objectSession2, UNVERSIONED);

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
            session1.commit();
            fail("session1.commit(...) invocation should fail.");
        } catch (final PersistentStorageException ex) {
            //attempted rollback should also fail:
            session1.rollback();
            fail("session1.rollback(...) invocation should fail.");
        }
    }

    private void mockOCFLObjectSession(final OCFLObjectSession objectSession, final CommitOption option) {
        when(objectSession.getDefaultCommitOption()).thenReturn(option);
        when(objectSession.getCreated()).thenReturn(Instant.now());

    }

    @Test
    public void getTriplesFailsIfCommitHasAlreadyStarted() throws Exception {
        final var ocflId = mintOCFLObjectId(RESOURCE_ID);
        mockMappingAndIndex(ocflId, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //mock success on commit for the first object session
        when(mockSessionFactory.create(eq(ocflId), anyString())).thenReturn(objectSession1);
        mockOCFLObjectSession(objectSession1, UNVERSIONED);
        //pause on commit for 1 second
        when(objectSession1.commit(any(CommitOption.class))).thenAnswer((Answer<String>) invocationOnMock -> {
            Thread.sleep(1000);
            return null;
        });

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
        } catch (final PersistentStorageException e){
            //do nothing
        }

        latch.await(1000, TimeUnit.MILLISECONDS);

        verify(objectSession1).commit(UNVERSIONED);
    }

    @Test
    public void rollbackSucceedsWhenPrepareFails() throws Exception {
        final var ocflId = mintOCFLObjectId(RESOURCE_ID);
        mockMappingAndIndex(ocflId, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);

        //mock success on commit for the first object session
        when(mockSessionFactory.create(eq(ocflId), anyString())).thenReturn(objectSession1);
        mockOCFLObjectSession(objectSession1, UNVERSIONED);

        //throw on prepare
        doThrow(new RuntimeException("prepare failure")).when(objectSession1).prepare();

        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        //persist the operation
        try {
            session1.persist(rdfSourceOperation);
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        try {
            session1.commit();
            fail("Operation should have failed.");
        } catch (final PersistentStorageException e) {
            //do nothing
        }

        session1.rollback();

        verify(objectSession1).close();
    }

    @Test(expected = PersistentStorageException.class)
    public void rollbackFailsWhenAlreadyCommitted() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);
        DefaultOCFLObjectSession.setGlobaDefaultCommitOption(NEW_VERSION);

        final PersistentStorageSession session1 = createSession(index, objectSessionFactory);
        //persist the operation
        try {
            session1.persist(rdfSourceOperation);
            session1.commit();
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        session1.rollback();
        fail("session1.rollback() invocation should have failed.");
    }

    @Test(expected = PersistentStorageException.class)
    public void rollbackSucceedsOnUncommittedChanges() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);
        final PersistentStorageSession session1 = createSession(index, this.objectSessionFactory);

        try {
            session1.persist(rdfSourceOperation);
            session1.rollback();
        }catch(final PersistentStorageException e) {
            fail("Neither persist() nor rollback() should have failed.");

        }
        //verify that the resource cannot be found now (since it wasn't committed).
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);
        newSession.getTriples(RESOURCE_ID, null);
        fail("second session.getTriples(...) invocation should have failed.");
    }

    @Test(expected = PersistentStorageException.class)
    public void rollbackFailsWhenAlreadyRolledBack() throws Exception {
        final String ocflId = mintOCFLObjectId(RESOURCE_ID);
        mockMappingAndIndex(ocflId, RESOURCE_ID, ROOT_OBJECT_ID, mapping);
        mockResourceOperation(rdfSourceOperation, RESOURCE_ID);
        when(mockSessionFactory.create(eq(ocflId), anyString())).thenReturn(objectSession1);
        doThrow(new PersistentStorageException("commit error")).when(objectSession1).commit(any(CommitOption.class));
        mockOCFLObjectSession(objectSession1, NEW_VERSION);
        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        try {
            session1.persist(rdfSourceOperation);
        } catch (final PersistentStorageException e) {
            fail("Operation should not fail.");
        }

        try {
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

        verify(objectSession1).close();

        session1.rollback();
    }


    //TODO implement this test once rollback is fully implemented.
    @Ignore
    @Test(expected = PersistentStorageException.class)
    public void rollbackFailsWhenInRollingBackState() throws Exception {

    }

    //TODO implement this test once rollback is fully implemented.
    @Ignore
    @Test(expected = PersistentStorageException.class)
    public void rollbackSucceedsWhenRollingBackCommittedVersions() throws Exception {
    }

    @Test
    public void getTriplesFromPreviousVersion() throws Exception {
        DefaultOCFLObjectSession.setGlobaDefaultCommitOption(NEW_VERSION);
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        final Node resourceUri = createURI(RESOURCE_ID);

        //mock the operation
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);

        mockResourceOperation(rdfSourceOperation, userStream, USER_PRINCIPAL, RESOURCE_ID);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to new version
        session.commit();


        //create a new session and verify that the state is the same
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //get the newly created version
        final List<Instant> versions = newSession.listVersions(RESOURCE_ID);
        final Instant version = versions.get(versions.size() - 1);

        //verify get triples from version
        final RdfStream retrievedUserStream = newSession.getTriples(RESOURCE_ID, version);
        final var node = createURI(RESOURCE_ID);
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());
    }

    @Test
    public void getBinaryContent() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);

        // commit to OCFL
        session.commit();

        // create a new session and verify the returned rdf stream.
        final var newSession = createSession(index, objectSessionFactory);
        final var result = IOUtils.toString(newSession.getBinaryContent(RESOURCE_ID, null), UTF_8);

        assertEquals(BINARY_CONTENT, result);
    }

    @Test
    public void getBinaryContentFailsIfAlreadyCommitted() throws Exception {
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);

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
        DefaultOCFLObjectSession.setGlobaDefaultCommitOption(NEW_VERSION);
        // SEE getTriplesFromPreviousVersion
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);
        // perform the create non-rdf source operation
        session.persist(binOperation);
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
        mockMappingAndIndex(mintOCFLObjectId(RESOURCE_ID), RESOURCE_ID, ROOT_OBJECT_ID, mapping);

        // create the binary
        final var binOperation = mockNonRdfSourceOperation(BINARY_CONTENT, USER_PRINCIPAL, RESOURCE_ID);

        // perform the create non-rdf source operation
        session.persist(binOperation);

        // create a new session and verify the returned rdf stream.
        final var result = IOUtils.toString(session.getBinaryContent(RESOURCE_ID, null), UTF_8);

        assertEquals(BINARY_CONTENT, result);
    }

    private NonRdfSourceOperation mockNonRdfSourceOperation(final String content,
            final String userPrincipal, final String resourceId) {
        final var binOperation = mock(NonRdfSourceOperation.class,
                withSettings().extraInterfaces(CreateResourceOperation.class));

        final var contentStream = new ByteArrayInputStream(content.getBytes());
        when(binOperation.getContentStream()).thenReturn(contentStream);
        when(binOperation.getContentSize()).thenReturn((long) content.length());
        when(binOperation.getResourceId()).thenReturn(resourceId);
        when(binOperation.getType()).thenReturn(CREATE);
        when(binOperation.getUserPrincipal()).thenReturn(userPrincipal);
        return binOperation;
    }
}
