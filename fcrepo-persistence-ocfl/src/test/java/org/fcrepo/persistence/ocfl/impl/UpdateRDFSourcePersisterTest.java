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
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
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
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateRDFSourcePersisterTest {

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");

    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String TITLE = "My title";

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private OcflObjectSession session;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Captor
    private ArgumentCaptor<InputStream> userTriplesIsCaptor;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private UpdateRdfSourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setup() throws Exception {
        operation = mock(RdfSourceOperation.class);

        when(psSession.getId()).thenReturn(SESSION_ID);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(operation.getType()).thenReturn(UPDATE);

        persister = new UpdateRdfSourcePersister(this.index);
    }

    @Test
    public void testHandle() {
        assertTrue(this.persister.handle(this.operation));
        final NonRdfSourceOperation badOperation = mock(NonRdfSourceOperation.class);
        assertFalse(this.persister.handle(badOperation));
    }

    @Test
    public void testPersistExistingResource() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getTriples()).thenReturn(userTriplesStream);

        // Setup headers of resource before this operation
        final var headers = newResourceHeaders(ROOT_RESOURCE_ID, RESOURCE_ID, BASIC_CONTAINER.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);
        when(session.readHeaders(anyString())).thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());

        final var originalCreation = headers.getCreatedDate();
        final var originalModified = headers.getLastModifiedDate();

        persister.persist(psSession, operation);

        verify(session).writeResource(headersCaptor.capture(), userTriplesIsCaptor.capture());

        // verify user triples
        final Model userModel = retrievePersistedUserModel();

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID.getResourceId()),
                DC.title, TITLE));

        // verify server triples
        final var resultHeaders = headersCaptor.getValue();

        assertEquals(BASIC_CONTAINER.toString(), resultHeaders.getInteractionModel());
        assertEquals(originalCreation, resultHeaders.getCreatedDate());
        // The relationship between the actual resource last modified date and the
        // client-asserted last modified data is unclear.
        assertTrue(originalModified.equals(resultHeaders.getLastModifiedDate())
                || originalModified.isBefore(resultHeaders.getLastModifiedDate()));
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
