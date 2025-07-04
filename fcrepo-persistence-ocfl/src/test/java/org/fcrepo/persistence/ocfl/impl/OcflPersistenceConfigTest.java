/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.ocfl.api.MutableOcflRepository;
import io.ocfl.api.exception.OcflIOException;
import org.fcrepo.config.DigestAlgorithm;
import org.fcrepo.config.MetricsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.config.Storage;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.validation.ObjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OcflPersistenceConfigTest {

    @Mock
    private OcflPropsConfig ocflPropsConfig;

    @Mock
    private MetricsConfig metricsConfig;

    @Mock
    private DataSource dataSource;

    @Mock
    private MeterRegistry meterRegistry;

    @Captor
    private ArgumentCaptor<S3AsyncClient> s3ClientCaptor;

    @Captor
    private ArgumentCaptor<S3AsyncClient> s3CrtClientCaptor;

    @InjectMocks
    private OcflPersistenceConfig ocflPersistenceConfig;

    @TempDir
    public Path tempDir;

    private Path repoRoot;
    private Path stagingDir;
    private Path tempDirPath;

    @BeforeEach
    public void setup() throws IOException {
        repoRoot = tempDir.resolve("ocfl-root");
        stagingDir = tempDir.resolve("staging");
        tempDirPath = tempDir.resolve("temp");

        Files.createDirectories(repoRoot);
        Files.createDirectories(stagingDir);
        Files.createDirectories(tempDirPath);

        // Default configuration for filesystem repository
        when(ocflPropsConfig.getStorage()).thenReturn(Storage.OCFL_FILESYSTEM);
        when(ocflPropsConfig.getOcflRepoRoot()).thenReturn(repoRoot);
        when(ocflPropsConfig.getOcflTemp()).thenReturn(tempDirPath);
        when(ocflPropsConfig.getFedoraOcflStaging()).thenReturn(stagingDir);
        when(ocflPropsConfig.getDefaultDigestAlgorithm()).thenReturn(DigestAlgorithm.SHA512);
        when(ocflPropsConfig.isOcflUpgradeOnWrite()).thenReturn(false);
        when(ocflPropsConfig.verifyInventory()).thenReturn(true);
        when(ocflPropsConfig.isUnsafeWriteEnabled()).thenReturn(false);
    }

    @Test
    public void testRepositoryCreationWithFilesystemStorage() throws IOException {
        final MutableOcflRepository repo = ocflPersistenceConfig.repository();

        assertNotNull(repo);
    }

    @Test
    public void testRepositoryCreationWithInvalidRepoRoot() throws Exception {
        // Set an invalid repository root (a file instead of directory)
        final Path invalidRoot = tempDir.resolve("invalid.txt");
        Files.write(invalidRoot, "test".getBytes());

        when(ocflPropsConfig.getOcflRepoRoot()).thenReturn(invalidRoot);

        assertThrows(OcflIOException.class, () -> ocflPersistenceConfig.repository());
    }

    @Test
    public void testObjectSessionFactoryCreation() throws IOException {
        // Configure for non-auto-versioning
        when(ocflPropsConfig.isAutoVersioningEnabled()).thenReturn(false);
        when(ocflPropsConfig.isResourceHeadersCacheEnabled()).thenReturn(true);
        when(ocflPropsConfig.getResourceHeadersCacheMaxSize()).thenReturn(100L);
        when(ocflPropsConfig.getResourceHeadersCacheExpireAfterSeconds()).thenReturn(60L);

        final OcflObjectSessionFactory factory = ocflPersistenceConfig.ocflObjectSessionFactory();

        assertNotNull(factory);
        assertInstanceOf(DefaultOcflObjectSessionFactory.class, factory);

        // Verify the commit type is unversioned
        assertEquals(CommitType.UNVERSIONED,
                ReflectionTestUtils.getField(factory, "defaultCommitType"));
    }

    @Test
    public void testObjectSessionFactoryWithAutoVersioning() throws IOException {
        // Configure for auto-versioning
        when(ocflPropsConfig.isAutoVersioningEnabled()).thenReturn(true);
        when(ocflPropsConfig.isResourceHeadersCacheEnabled()).thenReturn(true);
        when(ocflPropsConfig.getResourceHeadersCacheMaxSize()).thenReturn(100L);
        when(ocflPropsConfig.getResourceHeadersCacheExpireAfterSeconds()).thenReturn(60L);

        final OcflObjectSessionFactory factory = ocflPersistenceConfig.ocflObjectSessionFactory();

        assertNotNull(factory);
        // Verify the commit type is versioned
        assertEquals(CommitType.NEW_VERSION,
                ReflectionTestUtils.getField(factory, "defaultCommitType"));
    }

    @Test
    public void testObjectValidatorCreation() throws IOException {
        final ObjectValidator validator = ocflPersistenceConfig.objectValidator();

        assertNotNull(validator);
    }

    @Test
    public void testCreateCacheWithCachingAndMetricsEnabled() throws Exception {
        when(metricsConfig.isMetricsEnabled()).thenReturn(true);
        when(ocflPropsConfig.isResourceHeadersCacheEnabled()).thenReturn(true);
        when(ocflPropsConfig.getResourceHeadersCacheMaxSize()).thenReturn(100L);
        when(ocflPropsConfig.getResourceHeadersCacheExpireAfterSeconds()).thenReturn(60L);

        try (MockedStatic<Caffeine> caffeineMock = Mockito.mockStatic(Caffeine.class);
             MockedStatic<CaffeineCacheMetrics> metricsMock = Mockito.mockStatic(CaffeineCacheMetrics.class)) {
            final var caffeineMockBuilder = mock(Caffeine.class);
            caffeineMock.when(Caffeine::newBuilder).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.maximumSize(anyLong())).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.expireAfterAccess(anyLong(), any())).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.expireAfterAccess(any(Duration.class))).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.weakValues()).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.build()).thenReturn(mock(Cache.class));

            ocflPersistenceConfig.ocflObjectSessionFactory();
            verify(caffeineMockBuilder, times(2)).maximumSize(100L);
            verify(caffeineMockBuilder, times(2)).expireAfterAccess(60L, TimeUnit.SECONDS);
            verify(caffeineMockBuilder, times(4)).build();
            metricsMock.verify(() -> CaffeineCacheMetrics.monitor(
                    any(MeterRegistry.class), any(Cache.class), eq("resourceHeadersCache")));
            metricsMock.verify(() -> CaffeineCacheMetrics.monitor(
                    any(MeterRegistry.class), any(Cache.class), eq("rootIdCache")));
        }
    }

    @Test
    public void testCreateCacheWithCachingDisabled() throws Exception {
        when(ocflPropsConfig.isResourceHeadersCacheEnabled()).thenReturn(false);

        try (MockedStatic<Caffeine> caffeineMock = Mockito.mockStatic(Caffeine.class);
             MockedStatic<CaffeineCacheMetrics> metricsMock = Mockito.mockStatic(CaffeineCacheMetrics.class)) {
            final var caffeineMockBuilder = mock(Caffeine.class);
            caffeineMock.when(Caffeine::newBuilder).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.maximumSize(anyLong())).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.expireAfterAccess(anyLong(), any())).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.expireAfterAccess(any(Duration.class))).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.weakValues()).thenReturn(caffeineMockBuilder);
            when(caffeineMockBuilder.build()).thenReturn(mock(Cache.class));

            ocflPersistenceConfig.ocflObjectSessionFactory();
            verify(caffeineMockBuilder, times(2)).build();
            metricsMock.verify(() -> CaffeineCacheMetrics.monitor(
                    any(MeterRegistry.class), any(Cache.class), anyString()), never());
        }
    }

    @Test
    public void testS3RepositoryCreation() throws IOException {
        // Configure for S3 storage
        when(ocflPropsConfig.getStorage()).thenReturn(Storage.OCFL_S3);
        when(ocflPropsConfig.getOcflS3Bucket()).thenReturn("test-bucket");
        when(ocflPropsConfig.getOcflS3Prefix()).thenReturn("fedora/");
        when(ocflPropsConfig.isOcflS3DbEnabled()).thenReturn(true);

        when(ocflPropsConfig.getS3Endpoint()).thenReturn("https://s3.example.org");
        when(ocflPropsConfig.getAwsAccessKey()).thenReturn("testKey");
        when(ocflPropsConfig.getAwsSecretKey()).thenReturn("testSecret");
        when(ocflPropsConfig.isOcflS3ChecksumEnabled()).thenReturn(true);

        // Need to provide mock values for S3 client configuration
        when(ocflPropsConfig.getAwsRegion()).thenReturn("us-east-1");
        when(ocflPropsConfig.getS3ConnectionTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3ReadTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3WriteTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3MaxConcurrency()).thenReturn(100);

        try (final var ocflUtilsMock = Mockito.mockStatic(OcflPersistentStorageUtils.class)) {
            final var repoMock = mock(MutableOcflRepository.class);
            ocflUtilsMock.when(() -> OcflPersistentStorageUtils.createS3Repository(
                    any(DataSource.class), any(), any(), anyString(), anyString(), any(Path.class),
                    any(DigestAlgorithm.class), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(repoMock);

            ocflPersistenceConfig.repository();

            ocflUtilsMock.verify(() -> OcflPersistentStorageUtils.createS3Repository(
                    any(DataSource.class), any(S3AsyncClient.class), any(S3AsyncClient.class),
                    anyString(), anyString(), any(Path.class),
                    any(DigestAlgorithm.class), anyBoolean(), anyBoolean(), anyBoolean()
            ));
        }
    }

    @Test
    public void testS3RepositoryCreationWithPathStyleSupport() throws IOException {
        // Configure for S3 storage
        when(ocflPropsConfig.getStorage()).thenReturn(Storage.OCFL_S3);
        when(ocflPropsConfig.getOcflS3Bucket()).thenReturn("test-bucket");
        when(ocflPropsConfig.getOcflS3Prefix()).thenReturn("fedora/");
        when(ocflPropsConfig.isOcflS3DbEnabled()).thenReturn(true);

        when(ocflPropsConfig.getS3Endpoint()).thenReturn("https://s3.example.org");
        when(ocflPropsConfig.getAwsAccessKey()).thenReturn("testKey");
        when(ocflPropsConfig.getAwsSecretKey()).thenReturn("testSecret");
        when(ocflPropsConfig.isOcflS3ChecksumEnabled()).thenReturn(true);

        // Need to provide mock values for S3 client configuration
        when(ocflPropsConfig.getAwsRegion()).thenReturn("us-east-1");
        when(ocflPropsConfig.getS3ConnectionTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3ReadTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3WriteTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3MaxConcurrency()).thenReturn(100);
        when(ocflPropsConfig.isPathStyleAccessEnabled()).thenReturn(true);

        try (final var ocflUtilsMock = Mockito.mockStatic(OcflPersistentStorageUtils.class)) {
            final var repoMock = mock(MutableOcflRepository.class);
            ocflUtilsMock.when(() -> OcflPersistentStorageUtils.createS3Repository(
                    any(DataSource.class), any(), any(), anyString(), anyString(), any(Path.class),
                    any(DigestAlgorithm.class), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(repoMock);

            ocflPersistenceConfig.repository();

            ocflUtilsMock.verify(() -> OcflPersistentStorageUtils.createS3Repository(
                    any(DataSource.class), any(S3AsyncClient.class), any(S3AsyncClient.class),
                    anyString(), anyString(), any(Path.class),
                    any(DigestAlgorithm.class), anyBoolean(), anyBoolean(), anyBoolean()
            ));
        }
    }


    // ocflPropsConfig.isPathStyleAccessEnabled
}
