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
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.DefaultOCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Random;
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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

    private OCFLObjectSessionFactory objectSessionFactory;

    private String parentId = "info:fedora/parent";
    private String resourceId = parentId + "/child1";

    @Mock
    private RdfSourceOperation rdfSourceOperation;
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
                                                       final OCFLObjectSessionFactory objectSessionFactory){
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
        final Triple rdfSourceTriple = Triple.create(resourceUri, RDF.type.asNode(),RDF_SOURCE.asNode());
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
        final Node descriptionUri = createURI(descriptionResource);

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

}
