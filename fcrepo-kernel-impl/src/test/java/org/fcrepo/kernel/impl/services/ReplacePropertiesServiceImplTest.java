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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static java.util.stream.Stream.of;

import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.impl.operations.UpdateRdfSourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * DeleteResourceServiceTest
 *
 * @author bseeger
 */
@RunWith(MockitoJUnitRunner.Strict.class)
public class ReplacePropertiesServiceImplTest {

    @Mock
    private Transaction tx;

    @Mock
    private PersistentStorageSession pSession;

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    FedoraResource resource;

    @Mock
    private RdfSourceOperationBuilder builder;

    @InjectMocks
    private UpdateRdfSourceOperation operation;

    @InjectMocks
    private ReplacePropertiesServiceImpl service;

    private RdfSourceOperationFactory factory;

    private static final String FEDORA_URI = "http://www.example.com/fedora/rest/resource1";
    private static final String FEDORA_ID = "info:fedora/resource1";
    private static final String TX_ID = "tx-1234";
    private static final String CONTENT_TYPE = "text/turtle";

    private static final String RDF =
            "@prefix dc: <"  + DC.getURI() + "> ." +
            "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
            "<" + FEDORA_URI + "> dc:title 'fancy title' ;" +
            "<" + FEDORA_URI + "> dc:title 'another fancy title' ;";

    private final Node subject = createURI(FEDORA_ID);

    private final Triple triple1 = new Triple(createURI(FEDORA_ID),
        createURI("http://purl.org/dc/elements/1.1/title"),
        createLiteral("title one", XSDDatatype.XSDstring));
    private final Triple triple2 = new Triple(createURI(FEDORA_ID),
        createURI("http://purl.org/dc/elements/1.1/title"),
        createLiteral("title two", XSDDatatype.XSDstring));


    @Before
    public void setup() {
        when(tx.getId()).thenReturn(TX_ID);
        when(psManager.getSession(anyString())).thenReturn(pSession);
        when(resource.getId()).thenReturn(FEDORA_ID);
        when(factory.updateBuilder(eq(FEDORA_ID))).thenReturn(builder);
        when(builder.build()).thenReturn(operation);
    }

    @Test
    @Ignore
    public void testReplaceProperties() throws PersistentStorageException {
        final DefaultRdfStream rdfStream = new DefaultRdfStream(subject, of(triple1, triple2));
        when(resource.getTriples()).thenReturn(rdfStream);

        final Model model = ModelFactory.createDefaultModel();

        service.perform(tx.getId(), resource.getId(), CONTENT_TYPE, model);
        verify(pSession).persist(operation);
    }
}

