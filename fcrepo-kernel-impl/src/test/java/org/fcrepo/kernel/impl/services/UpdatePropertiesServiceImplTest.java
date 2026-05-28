/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test for UpdatePropertiesServiceImpl
 *
 * @author bbpennel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class UpdatePropertiesServiceImplTest {

    @Mock
    private ReplacePropertiesService replacePropertiesService;

    @Mock
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Mock
    private PersistentStorageSession persistentStorageSession;

    @Mock
    private Transaction transaction;

    @InjectMocks
    private UpdatePropertiesServiceImpl service;

    private FedoraId fedoraId;
    private String userPrincipal;
    private Model initialModel;
    private RdfStream initialTriples;

    @BeforeEach
    public void setup() {
        fedoraId = FedoraId.create("info:fedora/test-resource");
        userPrincipal = "testUser";

        // Create a simple RDF model with initial properties
        initialModel = createDefaultModel();
        final var resource = initialModel.createResource(fedoraId.getFullId());
        resource.addProperty(
                ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"),
                "Original Title");

        initialTriples = DefaultRdfStream.fromModel(resource.asNode(), initialModel);

        // Setup mocks
        when(persistentStorageSessionManager.getSession(transaction)).thenReturn(persistentStorageSession);
        when(persistentStorageSession.getTriples(fedoraId, null)).thenReturn(initialTriples);
    }

    @Test
    public void testUpdateProperties_Success() throws Exception {
        // SPARQL update to change the title
        final String sparqlUpdate =
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                        "DELETE { <" + fedoraId.getFullId() + "> dc:title \"Original Title\" }\n" +
                        "INSERT { <" + fedoraId.getFullId() + "> dc:title \"Updated Title\" }\n" +
                        "WHERE {}";

        // Call the service
        service.updateProperties(transaction, userPrincipal, fedoraId, sparqlUpdate);

        // Verify the replacePropertiesService was called with the correct parameters
        verify(replacePropertiesService).perform(eq(transaction), eq(userPrincipal), eq(fedoraId), any(Model.class));
    }

    @Test
    public void testUpdateProperties_ItemNotFound() throws Exception {
        // Setup service to throw PersistentItemNotFoundException
        doThrow(new PersistentItemNotFoundException("Not found"))
                .when(persistentStorageSession).getTriples(fedoraId, null);

        // SPARQL update doesn't matter for this test
        final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE {} INSERT {} WHERE {}";

        // Call the service and expect ItemNotFoundException
        assertThrows(ItemNotFoundException.class, () -> {
            service.updateProperties(transaction, userPrincipal, fedoraId, sparqlUpdate);
        });
    }

    @Test
    public void testUpdateProperties_StorageException() throws Exception {
        // Setup service to throw PersistentStorageException
        doThrow(new PersistentStorageException("Storage error"))
                .when(persistentStorageSession).getTriples(fedoraId, null);

        // SPARQL update doesn't matter for this test
        final String sparqlUpdate = "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE {} INSERT {} WHERE {}";

        // Call the service and expect RepositoryRuntimeException
        assertThrows(RepositoryRuntimeException.class, () -> {
            service.updateProperties(transaction, userPrincipal, fedoraId, sparqlUpdate);
        });
    }
}