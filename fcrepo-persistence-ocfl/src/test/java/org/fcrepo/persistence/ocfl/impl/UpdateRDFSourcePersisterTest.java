/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private Transaction transaction;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @BeforeEach
    public void setup() throws Exception {
        operation = mock(RdfSourceOperation.class);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(operation.getType()).thenReturn(UPDATE);
        when(operation.getTransaction()).thenReturn(transaction);

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
