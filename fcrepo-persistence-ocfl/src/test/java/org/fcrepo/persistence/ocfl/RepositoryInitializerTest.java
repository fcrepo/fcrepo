/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl;

import static org.fcrepo.config.ServerManagedPropsMode.STRICT;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.CreateVersionResourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.VersionResourceOperationFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RepositoryInitializerTest {

    @Mock
    private OcflPersistentSessionManager sessionManager;

    @Mock
    private RdfSourceOperationFactory operationFactory;

    @Mock
    private IndexBuilder indexBuilder;

    @Mock
    private VersionResourceOperationFactory versionResourceOperationFactory;

    @Mock
    private OcflPropsConfig ocflConfig;

    @Mock
    private FedoraPropsConfig fedoraPropsConfig;

    @Mock
    private TransactionManager txManager;

    @Mock
    private Transaction transaction;

    @Mock
    private PersistentStorageSession session;

    @Mock
    private CreateRdfSourceOperationBuilder operationBuilder;

    @Mock
    private CreateRdfSourceOperation operation;

    @Mock
    private CreateVersionResourceOperationBuilder versionOperationBuilder;

    @Mock
    private ResourceOperation versionOperation;

    @Mock
    private ContextRefreshedEvent event;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private RepositoryInitializationStatus initializationStatus;

    @InjectMocks
    private RepositoryInitializer initializer;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    @BeforeEach
    public void setUp() throws Exception {
        when(txManager.create()).thenReturn(transaction);
        when(sessionManager.getSession(transaction)).thenReturn(session);
        when(event.getApplicationContext()).thenReturn(applicationContext);
        when(fedoraPropsConfig.getServerManagedPropsMode()).thenReturn(STRICT);

        when(operationFactory.createBuilder(transaction, rootId, BASIC_CONTAINER.getURI(), STRICT))
                .thenReturn(operationBuilder);
        when(operationBuilder.parentId(rootId)).thenReturn(operationBuilder);
        when(operationBuilder.build()).thenReturn(operation);

        when(versionResourceOperationFactory.createBuilder(transaction, rootId))
                .thenReturn(versionOperationBuilder);
        when(versionOperationBuilder.build()).thenReturn(versionOperation);
    }

    @Test
    public void testInitializeWhenRootExists() throws Exception {
        // Test initialization when the root already exists
        initializer.initialize();

        verify(indexBuilder).rebuildIfNecessary();
        verify(txManager).create();
        verify(session).getHeaders(rootId, null);
        verify(session, never()).persist(any(RdfSourceOperation.class));
        verify(transaction, never()).commit();
    }

    @Test
    public void testInitializeWhenRootDoesNotExistWithAutoVersioningEnabled() throws Exception {
        // Test initialization when root doesn't exist and auto-versioning is enabled
        when(ocflConfig.isAutoVersioningEnabled()).thenReturn(true);
        doThrow(PersistentItemNotFoundException.class).when(session).getHeaders(rootId, null);



        initializer.initialize();

        verify(indexBuilder).rebuildIfNecessary();
        verify(session).persist(operation);
        verify(versionResourceOperationFactory, never()).createBuilder(any(), any());
        verify(transaction).commit();
    }

    @Test
    public void testInitializeWhenRootDoesNotExistWithoutAutoVersioningEnabled() throws Exception {
        // Test initialization when root doesn't exist and auto-versioning is disabled
        when(ocflConfig.isAutoVersioningEnabled()).thenReturn(false);
        doThrow(PersistentItemNotFoundException.class).when(session).getHeaders(rootId, null);

        initializer.initialize();

        verify(indexBuilder).rebuildIfNecessary();
        verify(session).persist(operation);
        verify(session).persist(versionOperation);
        verify(transaction).commit();
    }

    @Test
    public void testInitializeWithStorageException() throws Exception {
        // Test handling of PersistentStorageException
        doThrow(PersistentStorageException.class).when(session).getHeaders(rootId, null);

        assertThrows(RepositoryRuntimeException.class, () -> {
            initializer.initialize();
        });

        verify(indexBuilder).rebuildIfNecessary();
        verify(transaction, never()).commit();
    }

    @Test
    public void testOnApplicationEventSuccess() throws Exception {
        // Set up to avoid actual initialization
        doThrow(PersistentItemNotFoundException.class).when(session).getHeaders(rootId, null);

        when(operationFactory.createBuilder(any(), any(), any(), any()))
                .thenReturn(operationBuilder);
        when(operationBuilder.parentId(any())).thenReturn(operationBuilder);
        when(operationBuilder.build()).thenReturn(operation);

        // Should be false initially
        verify(initializationStatus, never()).setInitializationComplete(true);

        initializer.onApplicationEvent(event);

        // Should be true after event processing
        verify(initializationStatus).setInitializationComplete(true);
        verify(applicationContext, never()).close();
    }

    @Test
    public void testOnApplicationEventFailure() throws Exception {
        // Force an exception during initialization
        doThrow(RuntimeException.class).when(indexBuilder).rebuildIfNecessary();

        initializer.onApplicationEvent(event);

        // Even on failure, initialization should be marked complete
        verify(initializationStatus).setInitializationComplete(true);
        verify(applicationContext).close();
    }
}