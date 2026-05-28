/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createS3Repository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ocfl.api.MutableOcflRepository;
import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.extension.storage.layout.config.HashedNTupleLayoutConfig;
import io.ocfl.core.storage.OcflStorage;
import io.ocfl.core.storage.OcflStorageBuilder;
import org.apache.http.impl.auth.UnsupportedDigestAlgorithmException;
import org.apache.jena.riot.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class OcflPersistentStorageUtilsTest {
    @TempDir
    public Path tempDir;

    @Test
    public void testGetRdfFormat() {
        final RDFFormat format = OcflPersistentStorageUtils.getRdfFormat();
        assertEquals(RDFFormat.NTRIPLES, format);
    }

    @Test
    public void testGetRDFFileExtension() {
        final String extension = OcflPersistentStorageUtils.getRDFFileExtension();
        assertEquals(".nt", extension);
    }

    @Test
    public void testObjectMapper() {
        final var mapper = OcflPersistentStorageUtils.objectMapper();
        assertNotNull(mapper);

        // Test serialization/deserialization of dates
        final String json = mapper.createObjectNode()
                .put("timestamp", Instant.parse("2023-01-01T00:00:00Z").toString())
                .toString();
        assertEquals("{\"timestamp\":\"2023-01-01T00:00:00Z\"}", json);
    }

    @Test
    public void testTranslateFedoraDigestToOcfl() {
        final var sha512 = OcflPersistentStorageUtils.translateFedoraDigestToOcfl(
                org.fcrepo.config.DigestAlgorithm.SHA512);
        assertEquals("sha512", sha512.getOcflName());

        final var sha256 = OcflPersistentStorageUtils.translateFedoraDigestToOcfl(
                org.fcrepo.config.DigestAlgorithm.SHA256);
        assertEquals("sha256", sha256.getOcflName());
    }

    @Test
    public void testCreateFilesystemRepository() throws IOException {
        final var repoRoot = tempDir.resolve("ocfl-root");
        final var workDir = tempDir.resolve("work-dir");

        try (final var storageMockedConstruction =
                     mockConstruction(OcflStorageBuilder.class,
                             (mock, context) -> {
                                 when(mock.verifyInventoryDigest(anyBoolean())).thenReturn(mock);
                                 when(mock.fileSystem(any(Path.class))).thenReturn(mock);
                                 when(mock.build()).thenReturn(mock(OcflStorage.class));
                             });
             final var repoMockedConstruction =
                     mockConstruction(OcflRepositoryBuilder.class,
                             (mock, context) -> {
                                 when(mock.defaultLayoutConfig(any())).thenReturn(mock);
                                 when(mock.ocflConfig(any(Consumer.class))).thenReturn(mock);
                                 when(mock.logicalPathMapper(any())).thenReturn(mock);
                                 when(mock.workDir(any())).thenReturn(mock);
                                 when(mock.storage(any(Consumer.class))).thenReturn(mock);
                                 final var repository = mock(MutableOcflRepository.class);
                                 when(mock.buildMutable()).thenReturn(repository);
                             })
        ) {
            final var result = createFilesystemRepository(
                    repoRoot, workDir, org.fcrepo.config.DigestAlgorithm.SHA256,
                    true, true);

            // Verify repository was created
            assertNotNull(result);

            // Verify directories were created
            assertTrue(Files.exists(repoRoot));
            assertTrue(Files.exists(workDir));

            // Verify OcflStorageBuilder was configured correctly
            final var storageBuilder = storageMockedConstruction.constructed().get(0);
            verify(storageBuilder).verifyInventoryDigest(true);
            verify(storageBuilder).fileSystem(repoRoot);
            verify(storageBuilder).build();

            // Verify OcflRepositoryBuilder was configured
            final var repoBuilder = repoMockedConstruction.constructed().get(0);
            verify(repoBuilder).defaultLayoutConfig(any(HashedNTupleLayoutConfig.class));
            verify(repoBuilder).workDir(workDir);
            verify(repoBuilder).storage(any(OcflStorage.class));
        }
    }

    @Test
    public void testCreateFilesystemRepositoryWithInvalidDigest() {
        final var repoRoot = tempDir.resolve("ocfl-root");
        final var workDir = tempDir.resolve("work-dir");

        assertThrows(UnsupportedDigestAlgorithmException.class, () ->
                OcflPersistentStorageUtils.createFilesystemRepository(
                        repoRoot, workDir, org.fcrepo.config.DigestAlgorithm.MISSING,
                        true, true));
    }

    @Test
    public void testCreateS3Repository() throws IOException {
        final var workDir = tempDir.resolve("work-dir");
        final DataSource dataSource = mock(DataSource.class);
        final S3AsyncClient s3Client = mock(S3AsyncClient.class);
        final S3AsyncClient s3CrtClient = mock(S3AsyncClient.class);
        final String bucket = "test-bucket";
        final String prefix = "test-prefix";

        try (final var repoMockedConstruction =
                     mockConstruction(OcflRepositoryBuilder.class,
                             (mock, context) -> {
                                 when(mock.defaultLayoutConfig(any())).thenReturn(mock);
                                 when(mock.ocflConfig(any(Consumer.class))).thenReturn(mock);
                                 when(mock.logicalPathMapper(any())).thenReturn(mock);
                                 when(mock.workDir(any())).thenReturn(mock);
                                 when(mock.storage(any(Consumer.class))).thenReturn(mock);
                                 when(mock.contentPathConstraints(any())).thenReturn(mock);
                                 when(mock.objectDetailsDb(any(Consumer.class))).thenReturn(mock);
                                 final var repository = mock(MutableOcflRepository.class);
                                 when(mock.buildMutable()).thenReturn(repository);
                             });
        ) {
            final var result = createS3Repository(
                    dataSource, s3Client, s3CrtClient, bucket, prefix, workDir,
                    org.fcrepo.config.DigestAlgorithm.SHA512, true, false, true);

            // Verify repository was created
            assertNotNull(result);

            // Verify directory was created
            assertTrue(Files.exists(workDir));

            // Verify OcflRepositoryBuilder was configured
            final var repoBuilder = repoMockedConstruction.constructed().get(0);
            verify(repoBuilder).contentPathConstraints(any());
            verify(repoBuilder).storage(any(OcflStorage.class));
            verify(repoBuilder).objectDetailsDb(any(Consumer.class));
        }
    }
}