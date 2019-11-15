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
package org.fcrepo.persistence.ocfl;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.DefaultOCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link OCFLPersistentStorageSession}
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class OCFLPersistentStorageSessionTest {


    public static final String OCFL_OBJECT_ID = "ocfl-object-id";
    private OCFLPersistentStorageSession session;

    @Mock
    private FedoraToOCFLObjectIndex index;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private FedoraOCFLMapping mapping2;

    @Mock
    private OCFLObjectSessionFactory mockSessionFactory;

    @Mock
    private OCFLObjectSession objectSession1;

    @Mock
    private OCFLObjectSession objectSession2;

    private OCFLObjectSessionFactory objectSessionFactory;

    private String parentId = "info:fedora/parent";
    private String resourceId = parentId + "/child1";
    private String resourceId2 = "info:fedora/resource2";

    @Mock
    private RdfSourceOperation rdfSourceOperation;

    @Mock
    private RdfSourceOperation rdfSourceOperation2;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private ResourceOperation unsupportedOperation;

    @Before
    public void setUp() throws IOException {
        final var stagingDir = tempFolder.newFolder("ocfl-staging");
        final var repoDir = tempFolder.newFolder("ocfl-repo");
        final var workDir = tempFolder.newFolder("ocfl-work");

        this.objectSessionFactory = new DefaultOCFLObjectSessionFactory(stagingDir, repoDir, workDir);
        session = createSession(index, objectSessionFactory);
    }

    private OCFLPersistentStorageSession createSession(final FedoraToOCFLObjectIndex index,
                                                       final OCFLObjectSessionFactory objectSessionFactory) {
        return new OCFLPersistentStorageSession(new Random().nextLong() + "", index, objectSessionFactory);
    }

    @Test
    public void roundtripCreateContainerCreation() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("ocfl-object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        //create some test server props
        final Triple rdfSourceTriple = Triple.create(resourceUri, RDF.type.asNode(), RDF_SOURCE.asNode());
        final Stream<Triple> serverTriples = Stream.of(rdfSourceTriple);
        final RdfStream serverStream = new DefaultRdfStream(resourceUri, serverTriples);

        when(rdfSourceOperation.getTriples()).thenReturn(userStream);
        when(rdfSourceOperation.getServerManagedProperties()).thenReturn(serverStream);

        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        final var node = createURI(resourceId);

        //verify get triples within the transaction
        var retrievedUserStream = session.getTriples(resourceId, null);
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());

        //verify get server props within the transaction
        var retrievedServerStream = session.getManagedProperties(resourceId, null);
        assertEquals(node, retrievedServerStream.topic());
        assertEquals(rdfSourceTriple, retrievedServerStream.findFirst().get());

        //commit to OCFL
        session.commit(CommitOption.UNVERSIONED);

        //create a new session and verify that the state is the same
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //verify get triples outside of the transaction
        retrievedUserStream = newSession.getTriples(resourceId, null);
        assertEquals(node, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());

        //verify get server props outside of the transaction
        retrievedServerStream = newSession.getManagedProperties(resourceId, null);
        assertEquals(node, retrievedServerStream.topic());
        assertEquals(rdfSourceTriple, retrievedServerStream.findFirst().get());


    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedPersistOperation() throws Exception {
        this.session.persist(unsupportedOperation);
    }

    @Test
    public void getTriplesOfBinaryDescription() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("ocfl-object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);

        final String descriptionResource = resourceId + "/" + FedoraTypes.FCR_METADATA;
        when(index.getMapping(descriptionResource)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        //create some test user triples
        final String title = "my title";
        final var dcTitleTriple = Triple.create(resourceUri, DC.title.asNode(), createLiteral(title));
        final Stream<Triple> userTriples = Stream.of(dcTitleTriple);
        final DefaultRdfStream userStream = new DefaultRdfStream(resourceUri, userTriples);
        when(rdfSourceOperation.getTriples()).thenReturn(userStream);
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(descriptionResource);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to OCFL
        session.commit(CommitOption.UNVERSIONED);

        //create a new session and verify the returned rdf stream.
        final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);

        //verify get triples outside of the transaction
        final RdfStream retrievedUserStream = newSession.getTriples(descriptionResource, null);
        assertEquals(resourceUri, retrievedUserStream.topic());
        assertEquals(dcTitleTriple, retrievedUserStream.findFirst().get());
    }

    @Test
    public void persistFailsIfCommitAlreadyComplete() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn("ocfl-object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to OCFL
        session.commit(CommitOption.UNVERSIONED);

        //this should fail
        try {
            session.persist(rdfSourceOperation);
            fail("second session.persist(...) invocation should have failed.");
        } catch (PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void getTriplesFailsIfAlreadyCommitted() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn("ocfl-object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to OCFL
        session.commit(CommitOption.UNVERSIONED);

        //this should fail
        try {
            session.getTriples(resourceId, null);
            fail("second session.getTriples(...) invocation should have failed.");
        } catch (PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void commitFailsIfAlreadyCommitted() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn("ocfl-object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //commit to OCFL
        session.commit(CommitOption.UNVERSIONED);

        //this should fail
        try {
            session.commit(CommitOption.UNVERSIONED);
            fail("second session.commit(...) invocation should have failed.");
        } catch (PersistentStorageException ex) {
            //expected failure
        }
    }

    @Test
    public void verifyRollbackForUncommittedSessionSucceeds() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn(OCFL_OBJECT_ID);
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //perform the create rdf operation
        session.persist(rdfSourceOperation);

        //verify that getting triples within transaction succeeds
        session.getTriples(resourceId, null);

        //rollback
        session.rollback();

        //get triples should now fail because the session is effectively closed.
        try {
            session.getTriples(resourceId, null);
            fail("session.getTriples(...) invocation after rollback should have failed.");
        } catch (PersistentStorageException ex) {
            //expected failure
        }

        //verify that the resource cannot be found now (since it wasn't committed).
        try {
            final OCFLPersistentStorageSession newSession = createSession(index, objectSessionFactory);
            newSession.getTriples(resourceId, null);
            fail("second session.getTriples(...) invocation should have failed.");
        } catch (PersistentItemNotFoundException ex) {
            //expected failure
        }
    }

    /**
     * This test covers the expected behavior when two OCFL Object Sessions are modified and one of the commits to
     * the mutable head fails.
     *
     * @throws PersistentStorageException
     */
    @Test
    public void rollbackOnSessionWithCommitsToMutableHeadShouldFail() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn(OCFL_OBJECT_ID);
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final String ocfObjectId2 = OCFL_OBJECT_ID + "2";
        when(mapping2.getOcflObjectId()).thenReturn(ocfObjectId2);
        when(mapping2.getParentFedoraResourceId()).thenReturn(resourceId2);
        when(index.getMapping(resourceId2)).thenReturn(mapping2);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        final Node resourceUri2 = createURI(resourceId2);

        when(rdfSourceOperation2.getTriples()).thenReturn(new DefaultRdfStream(resourceUri2, Stream.empty()));
        when(rdfSourceOperation2.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri2, Stream.empty()));
        when(rdfSourceOperation2.getResourceId()).thenReturn(resourceId2);
        when(rdfSourceOperation2.getType()).thenReturn(CREATE);

        //mock success on commit for the first object session
        when(mockSessionFactory.create(eq(OCFL_OBJECT_ID), anyString())).thenReturn(objectSession1);
        when(objectSession1.commit(eq(CommitOption.UNVERSIONED))).thenReturn(Instant.now().toString());
        //mock failure on commit for the second object session
        when(mockSessionFactory.create(eq(ocfObjectId2), anyString())).thenReturn(objectSession2);
        when(objectSession2.commit(eq(CommitOption.UNVERSIONED))).thenThrow(PersistentStorageException.class);

        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        //persist the two operations
        session1.persist(rdfSourceOperation);

        //perform the create rdf operation
        session1.persist(rdfSourceOperation2);

        //get triples should now fail because the session is effectively closed.
        try {
            session1.commit(CommitOption.UNVERSIONED);
            fail("session1.commit(...) invocation should fail.");
        } catch (PersistentStorageException ex) {
            //attempted rollback should also fail:
            try {
                session1.rollback();
                fail("session1.rollback(...) invocation should fail.");
            } catch (final PersistentStorageException e) {
                assertTrue("exception does not contain expected text",
                        e.getMessage().contains("already committed to the unversioned "));
            }
        }
    }

    @Test
    public void getTriplesFailsIfCommitHasAlreadyStarted() throws PersistentStorageException {
        when(mapping.getOcflObjectId()).thenReturn(OCFL_OBJECT_ID);
        when(mapping.getParentFedoraResourceId()).thenReturn(parentId);
        when(index.getMapping(resourceId)).thenReturn(mapping);

        final Node resourceUri = createURI(resourceId);

        when(rdfSourceOperation.getTriples()).thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getServerManagedProperties())
                .thenReturn(new DefaultRdfStream(resourceUri, Stream.empty()));
        when(rdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(rdfSourceOperation.getType()).thenReturn(CREATE);

        //mock success on commit for the first object session
        when(mockSessionFactory.create(eq(OCFL_OBJECT_ID), anyString())).thenReturn(objectSession1);

        //pause on commit for 1 second
        when(objectSession1.commit(any(CommitOption.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread.sleep(1000);
                return null;
            }
        });

        final PersistentStorageSession session1 = createSession(index, mockSessionFactory);
        //persist the two operations
        session1.persist(rdfSourceOperation);

        new Thread(() -> {
            try {
                session1.commit(CommitOption.UNVERSIONED);
            } catch (PersistentStorageException e) {
            }
        }).start();

        //get triples should now fail because the commit has started.
        try {
            //sleep for a fraction of a second to ensure
            //commit thread runs first.
            Thread.sleep(500);
            session1.getTriples(resourceId, null);
            fail("session1.getTriples(...) invocation should fail.");
        } catch (Exception ex) {

        }
    }
}
