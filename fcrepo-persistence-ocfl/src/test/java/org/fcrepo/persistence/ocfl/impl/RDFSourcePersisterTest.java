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

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class RDFSourcePersisterTest {

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Captor
    private ArgumentCaptor<InputStream> userTriplesIsCaptor;

    @Captor
    private ArgumentCaptor<InputStream> serverTriplesIsCaptor;

    private final RDFSourcePersister persister = new RDFSourcePersister();

    @Test
    public void test() throws PersistentStorageException {
        final String resourceId = "info:fedora/parent/child";
        final String parentResourceId = "info:fedora/parent";

        final Node resourceUri = createURI(resourceId);
        //create some test user triples
        final String title = "my title";
        final Stream<Triple> userTriples = Stream.of(Triple.create(resourceUri,
                DC.title.asNode(),
                createLiteral(title)));
        final RdfStream userTriplesStream = new DefaultRdfStream(resourceUri, userTriples);

        //create some test server triples
        final Stream<Triple> serverTriples = Stream.of(Triple.create(resourceUri,
                RDF.type.asNode(),
                RDF_SOURCE.asNode()));
        final RdfStream serverTriplesStream = new DefaultRdfStream(resourceUri, serverTriples);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentResourceId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getTriples()).thenReturn(userTriplesStream);
        when(operation.getServerManagedProperties()).thenReturn(serverTriplesStream);
        persister.persist(session, operation, mapping);

        //verify user triples
        verify(session).write(eq("child" + getRDFFileExtension()), userTriplesIsCaptor.capture());
        final InputStream userTriplesIs = userTriplesIsCaptor.getValue();

        final Model userModel = createDefaultModel();
        RDFDataMgr.read(userModel, userTriplesIs, Lang.NTRIPLES);

        assertTrue(userModel.contains(userModel.createResource(resourceId),
                DC.title, title));

        //verify server triples
        verify(session).write(eq(getInternalFedoraDirectory() + "child" + getRDFFileExtension()), serverTriplesIsCaptor.capture());
        final InputStream serverTriplesIs = serverTriplesIsCaptor.getValue();

        final Model serverModel = createDefaultModel();
        RDFDataMgr.read(serverModel, serverTriplesIs, Lang.NTRIPLES);

        assertTrue(serverModel.containsLiteral(serverModel.createResource(resourceId),
                RDF.type, RDF_SOURCE));

    }
}
