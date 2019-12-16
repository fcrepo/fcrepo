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
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
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
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateRDFSourcePersisterTest {

    private static final String RESOURCE_ID = "info:fedora/parent/child";

    private static final String ROOT_RESOURCE_ID = "info:fedora/parent";

    private static final String USER_PRINCIPAL2 = "anotherUser";

    private static final Instant CREATED_DATE = Instant.parse("2019-11-22T12:10:04.000000Z");

    private static final Instant MODIFIED_DATE = Instant.parse("2019-11-22T12:40:33.697004Z");

    private static final String TITLE = "My title";

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private FedoraToOCFLObjectIndex index;

    @Mock
    private OCFLObjectSessionFactory sessionFactory;

    @Mock
    private OCFLPersistentStorageSession psSession;

    @Mock
    private WriteOutcome writeOutcome;

    @Captor
    private ArgumentCaptor<InputStream> userTriplesIsCaptor;

    @Captor
    private ArgumentCaptor<InputStream> headersIsCaptor;

    private CreateRDFSourcePersister persister;

    @Before
    public void setup() throws Exception {
        operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));

        when(session.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(anyString())).thenReturn(mapping);

        persister = new CreateRDFSourcePersister(this.sessionFactory, this.index );
    }

    @Test
    public void testPersistNewResource() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(ROOT_RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);
        persister.persist(psSession, operation);

        //verify user triples
        final Model userModel = retrievePersistedUserModel("child");

        assertTrue(userModel.contains(userModel.createResource(RESOURCE_ID),
                DC.title, TITLE));

        //verify server triples
        final var headers = retrievePersistedHeaders("child");

        assertEquals(RDF_SOURCE.toString(), headers.getInteractionModel());
    }

    @Test
    public void testPersistNewResourceOverrideRelaxed() throws Exception {
        final RdfStream userTriplesStream = constructTitleStream(RESOURCE_ID, TITLE);

        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(ROOT_RESOURCE_ID);

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(RDF_SOURCE.toString());
        when(operation.getTriples()).thenReturn(userTriplesStream);

        // Setting relaxed properties
        when(operation.getCreatedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedBy()).thenReturn(USER_PRINCIPAL2);
        when(operation.getLastModifiedDate()).thenReturn(MODIFIED_DATE);
        when(operation.getCreatedDate()).thenReturn(CREATED_DATE);

        persister.persist(psSession, operation);

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
