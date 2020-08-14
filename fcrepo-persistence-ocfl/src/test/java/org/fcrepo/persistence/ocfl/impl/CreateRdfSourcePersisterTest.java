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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.time.Instant;
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateRdfSourcePersisterTest {

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");

    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");

    private static final String USER_PRINCIPAL2 = "anotherUser";

    private static final Instant CREATED_DATE = Instant.parse("2019-11-22T12:10:04.000000Z");

    private static final Instant MODIFIED_DATE = Instant.parse("2019-11-22T12:40:33.697004Z");

    private static final String TITLE = "My title";

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private OcflObjectSession session;

    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Captor
    private ArgumentCaptor<InputStream> userTriplesIsCaptor;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private CreateRdfSourcePersister persister;

    @Before
    public void setup() throws Exception {
        operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(operation.getType()).thenReturn(CREATE);

        index = new TestOcflObjectIndex();

        persister = new CreateRdfSourcePersister(this.index);
    }

    @Test
    public void testHandle() {
        assertTrue(this.persister.handle(this.operation));
        final RdfSourceOperation badOperation = mock(RdfSourceOperation.class);
        when(badOperation.getType()).thenReturn(UPDATE);
        assertFalse(this.persister.handle(badOperation));
    }

    @Test
    public void testPersistNewResource() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(((CreateResourceOperation) operation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);

        persister.persist(psSession, operation);

        verify(session).writeResource(headersCaptor.capture(), userTriplesIsCaptor.capture());

        //verify user triples
        final Model userModel = retrievePersistedUserModel();

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID.getResourceId()),
                DC.title, TITLE));

        //verify server triples
        final var headers = headersCaptor.getValue();

        assertEquals(RDF_SOURCE.toString(), headers.getInteractionModel());
    }

    @Test
    public void testPersistNewResourceInExistingAg() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        final var parentHeaders = new ResourceHeadersImpl();
        parentHeaders.setArchivalGroup(true);

        final var ocflId = "ocfl-id-1";

        final String sessionId = "some-id";
        index.addMapping(sessionId, ROOT_RESOURCE_ID, ROOT_RESOURCE_ID, ocflId);
        index.commit(sessionId);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(((CreateResourceOperation) operation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);
        when(psSession.getHeaders(RESOURCE_ID, null)).thenReturn(null);
        when(psSession.getHeaders(ROOT_RESOURCE_ID, null)).thenReturn(parentHeaders);

        persister.persist(psSession, operation);

        verify(session).writeResource(headersCaptor.capture(), userTriplesIsCaptor.capture());

        //verify user triples
        final Model userModel = retrievePersistedUserModel();

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID.getResourceId()),
                DC.title, TITLE));

        //verify server triples
        final var headers = headersCaptor.getValue();

        assertEquals(RDF_SOURCE.toString(), headers.getInteractionModel());
        assertEquals(ocflId, index.getMapping(null, RESOURCE_ID).getOcflObjectId());
    }

    @Test
    public void testPersistNewResourceOverrideRelaxed() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(((CreateResourceOperation) operation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);


        // Setting relaxed properties
        when(operation.getCreatedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedDate()).thenReturn(MODIFIED_DATE);
        when(operation.getCreatedDate()).thenReturn(CREATED_DATE);

        persister.persist(psSession, operation);

        verify(session).writeResource(headersCaptor.capture(), userTriplesIsCaptor.capture());

        // verify user triples
        final Model userModel = retrievePersistedUserModel();

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID.getResourceId()),
                DC.title, TITLE));

        // verify server triples
        final var resultHeaders = headersCaptor.getValue();

        assertEquals(RDF_SOURCE.toString(), resultHeaders.getInteractionModel());

        assertEquals(MODIFIED_DATE, resultHeaders.getLastModifiedDate());
        assertEquals(CREATED_DATE, resultHeaders.getCreatedDate());
        assertEquals(USER_PRINCIPAL2, resultHeaders.getCreatedBy());
        assertEquals(USER_PRINCIPAL2, resultHeaders.getLastModifiedBy());
    }

    private RdfStream constructTitleStream(final FedoraId resourceId, final String title) {
        final Node resourceUri = createURI(resourceId.getResourceId());
        // create some test user triples
        final Stream<Triple> userTriples = Stream.of(Triple.create(resourceUri,
                DC.title.asNode(),
                createLiteral(title)));
        return new DefaultRdfStream(resourceUri, userTriples);
    }

    private Model retrievePersistedUserModel() throws Exception {
        final InputStream userTriplesIs = userTriplesIsCaptor.getValue();
        final Model userModel = createDefaultModel();
        RDFDataMgr.read(userModel, userTriplesIs, Lang.NTRIPLES);
        return userModel;
    }

}
