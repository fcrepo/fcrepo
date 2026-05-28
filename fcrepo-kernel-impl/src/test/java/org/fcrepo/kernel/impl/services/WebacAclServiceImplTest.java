/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_WEBAC_ACL_URI;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.impl.models.WebacAclImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for WebacAclServiceImpl
 *
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WebacAclServiceImplTest {

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Mock
    private FedoraPropsConfig fedoraPropsConfig;

    @Mock
    private Cache<String, Optional<ACLHandle>> authHandleCache;

    @Mock
    private Transaction transaction;

    @Mock
    private WebacAclImpl acl;

    @Mock
    private CreateRdfSourceOperationBuilder builder;

    @Mock
    private ResourceHeaders parentHeaders;

    @Mock
    private CreateRdfSourceOperation operation;

    @InjectMocks
    private WebacAclServiceImpl service;

    @Captor
    private ArgumentCaptor<FedoraId> idCaptor;

    @Mock
    private EventAccumulator eventAccumulator;

    private FedoraId resourceId;
    private FedoraId aclId;
    private String userPrincipal;
    private Model aclModel;
    private Resource aclResource;

    @BeforeEach
    public void setup() {
        resourceId = FedoraId.create(UUID.randomUUID().toString());
        aclId = resourceId.asAcl();
        userPrincipal = "testuser";

        aclModel = createDefaultModel();
        aclResource = aclModel.createResource(aclId.getFullId());

        // Setup persistence session
        when(psManager.getSession(transaction)).thenReturn(psSession);
        final var parentId = resourceId.asBaseId();
        when(psSession.getHeaders(parentId, null)).thenReturn(parentHeaders);

        // Setup operation building
        when(rdfSourceOperationFactory.createBuilder(any(), any(), any(), any()))
                .thenReturn(builder);
        when(builder.parentId(any())).thenReturn(builder);
        when(builder.triples(any())).thenReturn(builder);
        when(builder.relaxedProperties(any())).thenReturn(builder);
        when(builder.userPrincipal(any())).thenReturn(builder);
        when(builder.build()).thenReturn(operation);

        // Setup props config
        when(fedoraPropsConfig.getServerManagedPropsMode())
                .thenReturn(ServerManagedPropsMode.RELAXED);
    }

    @Test
    public void testFind_Success() throws Exception {
        when(resourceFactory.getResource(transaction, aclId, WebacAclImpl.class))
                .thenReturn(acl);

        final WebacAcl result = service.find(transaction, aclId);

        assertEquals(acl, result);
    }

    @Test
    public void testFind_NotFound() throws Exception {
        when(resourceFactory.getResource(transaction, aclId, WebacAclImpl.class))
                .thenThrow(new PathNotFoundException("Not found"));

        assertThrows(PathNotFoundRuntimeException.class, () -> {
            service.find(transaction, aclId);
        });
    }

    @Test
    public void testCreate_Success() throws Exception {
        final Resource authResource = aclModel.createResource("info:fedora/auth1/fcr:acl#");
        authResource.addProperty(
                aclModel.createProperty("http://www.w3.org/ns/auth/acl#mode"),
                aclModel.createResource("http://www.w3.org/ns/auth/acl#Read"));

        aclResource.addProperty(
                aclModel.createProperty("http://www.w3.org/ns/auth/acl#Authorization"),
                authResource);

        service.create(transaction, aclId, userPrincipal, aclModel);

        // Verify operation created correctly
        verify(rdfSourceOperationFactory).createBuilder(transaction, aclId, FEDORA_WEBAC_ACL_URI,
                ServerManagedPropsMode.RELAXED);
        verify(builder).parentId(resourceId);
        verify(builder).userPrincipal(userPrincipal);
        verify(builder).build();

        // Verify transaction locking
        verify(transaction).lockResource(aclId);

        // Verify operation persisted
        verify(psSession).persist(operation);

        // Verify auth handle cache invalidated
        verify(authHandleCache).invalidateAll();
    }

    @Test
    public void testCreate_PersistenceFailure() throws Exception {
        doThrow(new PersistentStorageException("Failed"))
                .when(psSession).persist(any(RdfSourceOperation.class));

        assertThrows(RepositoryRuntimeException.class, () -> {
            service.create(transaction, aclId, userPrincipal, aclModel);
        });
    }

    @Test
    public void testEnsureValidACLAuthorization_NoAuthorization() {
        // Create a model without authorization
        final Model invalidModel = createDefaultModel();
        final Resource targetResource = invalidModel.createResource("info:fedora/target");
        final Resource authResource = invalidModel.createResource("info:fedora/auth1/fcr:acl#");
        authResource.addProperty(invalidModel.createProperty(WEBAC_ACCESS_TO), targetResource);
        authResource.addProperty(invalidModel.createProperty(WEBAC_ACCESS_TO_CLASS), targetResource);

        assertThrows(RepositoryRuntimeException.class, () -> {
            service.create(transaction, aclId, userPrincipal, invalidModel);
        });
    }
}