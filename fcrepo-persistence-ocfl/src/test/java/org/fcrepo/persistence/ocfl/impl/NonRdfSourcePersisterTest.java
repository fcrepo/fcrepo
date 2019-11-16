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

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.DEFAULT_RDF_EXTENSION;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.INTERNAL_FEDORA_DIRECTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
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
@RunWith(MockitoJUnitRunner.class)
public class NonRdfSourcePersisterTest {

    @Mock
    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<InputStream> serverTriplesIsCaptor;

    private static final String resourceId = "info:fedora/parent/child";
    private static final String parentResourceId = "info:fedora/parent";

    private final NonRdfSourcePersister persister = new NonRdfSourcePersister();

    @Before
    public void setUp() {
        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(parentResourceId);
    }

    @Test
    public void testNonRdf() throws Exception {

        final Node resourceUri = createURI(resourceId);
        final String inputContent = "this is some example content";
        final InputStream content = IOUtils.toInputStream(inputContent, "UTF-8");

        //create some test server triples
        final Stream<Triple> serverTriples = Stream.of(Triple.create(resourceUri,
                RDF.type.asNode(),
                NON_RDF_SOURCE.asNode()));
        final RdfStream serverTriplesStream = new DefaultRdfStream(resourceUri, serverTriples);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(resourceId);
        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getServerManagedProperties()).thenReturn(serverTriplesStream);

        persister.persist(session, nonRdfSourceOperation, mapping);

        // verify user content
        verify(session).write(eq("child"), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(inputContent, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        //verify server triples
        verify(session).write(eq(INTERNAL_FEDORA_DIRECTORY + "/child" + DEFAULT_RDF_EXTENSION), serverTriplesIsCaptor.capture());
        final InputStream serverTriplesIs = serverTriplesIsCaptor.getValue();

        final Model serverModel = createDefaultModel();
        RDFDataMgr.read(serverModel, serverTriplesIs, Lang.NTRIPLES);

        assertTrue(serverModel.containsLiteral(serverModel.createResource(resourceId),
                RDF.type, NON_RDF_SOURCE));
    }
}
