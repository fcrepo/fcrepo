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
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.api.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.api.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.InputStream;
import java.time.Instant;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.junit.Before;
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

    private static final String RESOURCE_ID = "info:fedora/parent/child";

    private static final String PARENT_RESOURCE_ID = "info:fedora/parent";

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String USER_PRINCIPAL2 = "anotherUser";

    private static final Instant CREATED_DATE = Instant.parse("2019-11-22T12:10:04.000000Z");

    private static final Instant MODIFIED_DATE = Instant.parse("2019-11-22T12:40:33.697004Z");

    private static final String TITLE = "My title";

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private PersistentStorageSession storageSession;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Captor
    private ArgumentCaptor<InputStream> userTriplesIsCaptor;

    @Captor
    private ArgumentCaptor<InputStream> headersIsCaptor;

    private final RDFSourcePersister persister = new RDFSourcePersister();

    @Before
    public void setup() {
        operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
    }

    @Test
    public void testPersistNewResource() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(PARENT_RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);
        persister.persist(storageSession, session, operation, mapping);

        //verify user triples
        final Model userModel = retrievePersistedUserModel("child");

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID),
                DC.title, TITLE));

        //verify server triples
        final var headers = retrievePersistedHeaders("child");

        assertEquals(RDF_SOURCE.toString(), headers.getInteractionModel());
    }

    @Test
    public void testPersistExistingResource() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(PARENT_RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getType()).thenReturn(UPDATE);
        when(operation.getTriples()).thenReturn(userTriplesStream);

        // Setup headers of resource before this operation
        final var headers = newResourceHeaders(PARENT_RESOURCE_ID, RESOURCE_ID, BASIC_CONTAINER.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);
        when(storageSession.getHeaders(RESOURCE_ID, null)).thenReturn(headers);

        final var originalCreation = headers.getCreatedDate();
        final var originalModified = headers.getLastModifiedDate();

        persister.persist(storageSession, session, operation, mapping);

        // verify user triples
        final Model userModel = retrievePersistedUserModel("child");

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID),
                DC.title, TITLE));

        // verify server triples
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(BASIC_CONTAINER.toString(), resultHeaders.getInteractionModel());
        assertEquals(originalCreation, resultHeaders.getCreatedDate());
        assertTrue(originalModified.isBefore(resultHeaders.getLastModifiedDate()));
    }

    @Test
    public void testPersistNewResourceOverrideRelaxed() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getParentFedoraResourceId()).thenReturn(PARENT_RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);

        // Setting relaxed properties
        when(operation.getCreatedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedDate()).thenReturn(MODIFIED_DATE);
        when(operation.getCreatedDate()).thenReturn(CREATED_DATE);

        persister.persist(storageSession, session, operation, mapping);

        // verify user triples
        final Model userModel = retrievePersistedUserModel("child");

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID),
                DC.title, TITLE));

        // verify server triples
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(RDF_SOURCE.toString(), resultHeaders.getInteractionModel());

        assertEquals(MODIFIED_DATE, resultHeaders.getLastModifiedDate());
        assertEquals(CREATED_DATE, resultHeaders.getCreatedDate());
        assertEquals(USER_PRINCIPAL2, resultHeaders.getCreatedBy());
        assertEquals(USER_PRINCIPAL2, resultHeaders.getLastModifiedBy());
    }

    private RdfStream constructTitleStream(final String resourceId, final String title) {
        final Node resourceUri = createURI(resourceId);
        // create some test user triples
        final Stream<Triple> userTriples = Stream.of(Triple.create(resourceUri,
                DC.title.asNode(),
                createLiteral(title)));
        return new DefaultRdfStream(resourceUri, userTriples);
    }

    private ResourceHeaders retrievePersistedHeaders(final String subpath) throws Exception {
        verify(session).write(eq(getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION),
                headersIsCaptor.capture());
        final var headersIs = headersIsCaptor.getValue();
        return deserializeHeaders(headersIs);
    }

    private Model retrievePersistedUserModel(final String subpath) throws Exception {
        verify(session).write(eq("child" + getRDFFileExtension()), userTriplesIsCaptor.capture());
        final InputStream userTriplesIs = userTriplesIsCaptor.getValue();

        final Model userModel = createDefaultModel();
        RDFDataMgr.read(userModel, userTriplesIs, Lang.NTRIPLES);
        return userModel;
    }
}
