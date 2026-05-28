/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test for {@link OcflPropsConfig}
 *
 * @author bbpennel
 */
public class OcflPropsConfigTest {

    private OcflPropsConfig config;
    private GenericApplicationContext context;
    private MockEnvironment env;

    @BeforeEach
    public void setUp() {
        config = new OcflPropsConfig();
        env = new MockEnvironment();
    }

    @AfterEach
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    private void initializeContext() {
        context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);
        context.registerBean(FedoraPropsConfig.class);
        context.registerBean(OcflPropsConfig.class);
    }

    private void initializeConfig() {
        context.refresh();
        config = context.getBean(OcflPropsConfig.class);
    }

    @Test
    public void testDefaultValues() {
        env.setProperty("fcrepo.home", "fcrepo-home");
        initializeContext();
        initializeConfig();

        // Default paths
        assertEquals(Paths.get("fcrepo-home/data/staging"), config.getFedoraOcflStaging());
        assertEquals(Paths.get("fcrepo-home/data/ocfl-root"), config.getOcflRepoRoot());
        assertEquals(Paths.get("fcrepo-home/data/ocfl-temp"), config.getOcflTemp());

        // Default configurations
        assertTrue(config.isAutoVersioningEnabled());
        assertEquals(Storage.OCFL_FILESYSTEM, config.getStorage());
        assertTrue(config.isResourceHeadersCacheEnabled());
        assertEquals(512, config.getResourceHeadersCacheMaxSize());
        assertEquals(600, config.getResourceHeadersCacheExpireAfterSeconds());
        assertTrue(config.isReindexFailOnError());
        assertEquals(100, config.getReindexBatchSize());
        assertEquals(DigestAlgorithm.SHA512, config.getDefaultDigestAlgorithm());
        assertFalse(config.isPathStyleAccessEnabled());
        assertTrue(config.isOcflS3DbEnabled());
        assertFalse(config.isUnsafeWriteEnabled());
        assertEquals(1024, config.getFedoraToOcflCacheSize());
        assertEquals(30, config.getFedoraToOcflCacheTimeout());
        assertFalse(config.isOcflUpgradeOnWrite());
        assertTrue(config.verifyInventory());
        assertEquals(60, config.getS3ConnectionTimeout());
        assertEquals(60, config.getS3WriteTimeout());
        assertEquals(60, config.getS3ReadTimeout());
        assertEquals(100, config.getS3MaxConcurrency());
        assertTrue(config.isOcflS3ChecksumEnabled());
        assertFalse(config.isShowPath());
    }

    @Test
    public void testSetters() {
        // Test path setters
        final Path newStaging = Paths.get("/new/staging");
        config.setFedoraOcflStaging(newStaging);
        assertEquals(newStaging, config.getFedoraOcflStaging());

        final Path newRoot = Paths.get("/new/root");
        config.setOcflRepoRoot(newRoot);
        assertEquals(newRoot, config.getOcflRepoRoot());

        final Path newTemp = Paths.get("/new/temp");
        config.setOcflTemp(newTemp);
        assertEquals(newTemp, config.getOcflTemp());

        // Test config setters
        config.setAutoVersioningEnabled(false);
        assertFalse(config.isAutoVersioningEnabled());

        config.setStorage(Storage.OCFL_S3);
        assertEquals(Storage.OCFL_S3, config.getStorage());

        config.setAwsAccessKey("testAccessKey");
        assertEquals("testAccessKey", config.getAwsAccessKey());

        config.setAwsSecretKey("testSecretKey");
        assertEquals("testSecretKey", config.getAwsSecretKey());

        config.setAwsRegion("us-west-2");
        assertEquals("us-west-2", config.getAwsRegion());

        config.setOcflS3Bucket("test-bucket");
        assertEquals("test-bucket", config.getOcflS3Bucket());

        config.setOcflS3Prefix("test-prefix");
        assertEquals("test-prefix", config.getOcflS3Prefix());

        config.setResourceHeadersCacheEnabled(false);
        assertFalse(config.isResourceHeadersCacheEnabled());

        config.setResourceHeadersCacheMaxSize(1000);
        assertEquals(1000, config.getResourceHeadersCacheMaxSize());

        config.setResourceHeadersCacheExpireAfterSeconds(1800);
        assertEquals(1800, config.getResourceHeadersCacheExpireAfterSeconds());

        config.setReindexingThreads(5);
        assertEquals(5, config.getReindexingThreads());

        config.setReindexBatchSize(200);
        assertEquals(200, config.getReindexBatchSize());

        config.setReindexFailOnError(false);
        assertFalse(config.isReindexFailOnError());
    }

    @Test
    public void testReindexThreadsWithZero() {
        env.setProperty("fcrepo.ocfl.reindex.threads", "0");
        initializeContext();
        initializeConfig();

        assertEquals(1, config.getReindexingThreads());
    }

    @Test
    public void testReindexThreadsWithNegative() {
        env.setProperty("fcrepo.ocfl.reindex.threads", "-1");
        initializeContext();
        initializeConfig();

        // Should be set to default value (CPU cores - 1, minimum 1)
        final long expected = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        assertEquals(expected, config.getReindexingThreads());
    }

    @Test
    public void testReindexThreadsWithPositive() {
        env.setProperty("fcrepo.ocfl.reindex.threads", "5");
        initializeContext();
        initializeConfig();

        assertEquals(5, config.getReindexingThreads());
    }

    @Test
    public void testS3StorageWithoutBucket() {
        env.setProperty("fcrepo.storage", "ocfl-s3");
        initializeContext();

        // Should throw exception when S3 storage is configured without a bucket
        final var exception = assertThrows(BeanCreationException.class, this::initializeConfig);
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    public void testS3StorageWithBucket() {
        env.setProperty("fcrepo.storage", "ocfl-s3");
        env.setProperty("fcrepo.ocfl.s3.bucket", "test-bucket");
        initializeContext();
        initializeConfig();

        assertEquals(Storage.OCFL_S3, config.getStorage());
        assertEquals("test-bucket", config.getOcflS3Bucket());
    }

    @Test
    public void testInvalidDigestAlgorithm() {
        env.setProperty("fcrepo.persistence.defaultDigestAlgorithm", "md5");
        initializeContext();

        // Should throw exception when an invalid digest algorithm is provided
        final var exception = assertThrows(BeanCreationException.class, this::initializeConfig);
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    public void testValidDigestAlgorithm() {
        env.setProperty("fcrepo.persistence.defaultDigestAlgorithm", "sha256");
        initializeContext();
        initializeConfig();

        assertEquals(DigestAlgorithm.SHA256, config.getDefaultDigestAlgorithm());
    }

    @Test
    public void testOverrideDefaultPaths() {
        env.setProperty("fcrepo.home", "custom/home");
        env.setProperty("fcrepo.ocfl.staging", "custom/staging");
        env.setProperty("fcrepo.ocfl.root", "custom/root");
        env.setProperty("fcrepo.ocfl.temp", "custom/temp");

        initializeContext();
        initializeConfig();

        assertEquals(Paths.get("custom/staging"), config.getFedoraOcflStaging());
        assertEquals(Paths.get("custom/root"), config.getOcflRepoRoot());
        assertEquals(Paths.get("custom/temp"), config.getOcflTemp());
    }
}