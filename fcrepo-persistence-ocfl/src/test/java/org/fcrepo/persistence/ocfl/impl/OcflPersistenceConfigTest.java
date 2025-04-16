/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

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
    public void testS3ClientCreation() throws Exception {
        when(ocflPropsConfig.getAwsRegion()).thenReturn("us-east-1");
        when(ocflPropsConfig.getS3Endpoint()).thenReturn("https://s3.example.org");
        when(ocflPropsConfig.isPathStyleAccessEnabled()).thenReturn(true);
        when(ocflPropsConfig.getAwsAccessKey()).thenReturn("testKey");
        when(ocflPropsConfig.getAwsSecretKey()).thenReturn("testSecret");
        when(ocflPropsConfig.getS3ConnectionTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3ReadTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3WriteTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3MaxConcurrency()).thenReturn(100);

        final var s3ClientMethod = OcflPersistenceConfig.class.getDeclaredMethod("s3Client");
        s3ClientMethod.setAccessible(true);

        assertDoesNotThrow(() -> s3ClientMethod.invoke(ocflPersistenceConfig));
    }

    @Test
    public void testS3CrtClientCreation() throws Exception {
        when(ocflPropsConfig.getAwsRegion()).thenReturn("us-east-1");
        when(ocflPropsConfig.getS3Endpoint()).thenReturn("https://s3.example.org");
        when(ocflPropsConfig.getAwsAccessKey()).thenReturn("testKey");
        when(ocflPropsConfig.getAwsSecretKey()).thenReturn("testSecret");
        when(ocflPropsConfig.isOcflS3ChecksumEnabled()).thenReturn(true);

        final var s3CrtClientMethod = OcflPersistenceConfig.class.getDeclaredMethod("s3CrtClient");
        s3CrtClientMethod.setAccessible(true);

        assertDoesNotThrow(() -> s3CrtClientMethod.invoke(ocflPersistenceConfig));
    }

    @Test
    public void testS3RepositoryCreation() throws IOException {
        // Configure for S3 storage
        when(ocflPropsConfig.getStorage()).thenReturn(Storage.OCFL_S3);
        when(ocflPropsConfig.getOcflS3Bucket()).thenReturn("test-bucket");
        when(ocflPropsConfig.getOcflS3Prefix()).thenReturn("fedora/");
        when(ocflPropsConfig.isOcflS3DbEnabled()).thenReturn(true);

        // Need to provide mock values for S3 client configuration
        when(ocflPropsConfig.getAwsRegion()).thenReturn("us-east-1");
        when(ocflPropsConfig.getS3ConnectionTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3ReadTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3WriteTimeout()).thenReturn(30);
        when(ocflPropsConfig.getS3MaxConcurrency()).thenReturn(100);

        // This will fail in the S3 client creation since we can't fully mock AWS services,
        // but we can verify that the right path is taken
        assertThrows(NullPointerException.class, () -> ocflPersistenceConfig.repository());
    }
}