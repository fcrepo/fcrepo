/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OCFL implementation of PersistentStorageSessionManager
 *
 * @author whikloj
 * @author dbernstein
 * @since 2019-09-20
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class OcflPersistentSessionManager implements PersistentStorageSessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPersistentSessionManager.class);

    private volatile PersistentStorageSession readOnlySession;

    private Map<String, PersistentStorageSession> sessionMap;

    @Inject
    private OcflObjectSessionFactory objectSessionFactory;

    @Inject
    private FedoraToOcflObjectIndex ocflIndex;

    @Inject
    private ReindexService reindexService;

    @Inject
    private OcflPropsConfig ocflPropsConfig;

    /**
     * Default constructor
     */
    @Autowired
    public OcflPersistentSessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
    }

    @Override
    public PersistentStorageSession getSession(final Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("session id must be non-null");
        }

        return sessionMap.computeIfAbsent(transaction.getId(), key -> {
            LOGGER.debug("Creating storage session {}", transaction);
            return new OcflPersistentStorageSessionMetrics(
                    new OcflPersistentStorageSession(
                            transaction,
                            ocflIndex,
                            objectSessionFactory,
                            reindexService));
        });
    }

    @Override
    public PersistentStorageSession getReadOnlySession() {
        var localSession = this.readOnlySession;

        if (localSession == null) {
            synchronized (this) {
                localSession = this.readOnlySession;
                if (localSession == null) {
                    this.readOnlySession = new OcflPersistentStorageSessionMetrics(
                            new OcflPersistentStorageSession(ReadOnlyTransaction.INSTANCE,
                                    ocflIndex, objectSessionFactory, reindexService));
                    localSession = this.readOnlySession;
                }
            }
        }

        return localSession;
    }

    @Override
    public PersistentStorageSession removeSession(final String sessionId) {
        LOGGER.debug("Removing storage session {}", sessionId);
        return sessionMap.remove(sessionId);
    }

    @Override
    public void clearAllSessions() {
        LOGGER.debug("Clearing all storage sessions");
        sessionMap.clear();
        ocflIndex.clearAllTransactions();
        try {
            deleteStagingDirectories();
        } catch (IOException e) {
            LOGGER.error("Failed to delete OCFL staging directories", e);
        }
    }

    /**
     * Deletes all of the staging directories within the root staging directory
     * @throws IOException
     */
    private void deleteStagingDirectories() throws IOException {
        // Delete
        Files.walkFileTree(ocflPropsConfig.getFedoraOcflStaging(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                // Delete the file
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                // Delete the directory after its contents have been deleted, excluding the root staging directory
                if (!dir.equals(ocflPropsConfig.getFedoraOcflStaging())) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
