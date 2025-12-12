/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createS3Repository;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.config.MetricsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.config.Storage;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.validation.ObjectValidator;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.cache.Cache;
import org.fcrepo.storage.ocfl.cache.CaffeineCache;
import org.fcrepo.storage.ocfl.cache.NoOpCache;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.ocfl.api.MutableOcflRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Role;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * A Configuration for OCFL dependencies
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class OcflPersistenceConfig {

    @Inject
    private OcflPropsConfig ocflPropsConfig;

    @Inject
    private MetricsConfig metricsConfig;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private DataSource dataSource;

    /**
     * Create an OCFL Repository
     * @return the repository
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MutableOcflRepository repository() throws IOException {
        if (ocflPropsConfig.getStorage() == Storage.OCFL_S3) {
            return createS3Repository(
                    dataSource,
                    s3Client(),
                    s3CrtClient(),
                    ocflPropsConfig.getOcflS3Bucket(),
                    ocflPropsConfig.getOcflS3Prefix(),
                    ocflPropsConfig.getOcflTemp(),
                    ocflPropsConfig.getDefaultDigestAlgorithm(),
                    ocflPropsConfig.isOcflS3DbEnabled(),
                    ocflPropsConfig.isOcflUpgradeOnWrite(),
                    ocflPropsConfig.verifyInventory());
        } else {
            return createFilesystemRepository(ocflPropsConfig.getOcflRepoRoot(), ocflPropsConfig.getOcflTemp(),
                    ocflPropsConfig.getDefaultDigestAlgorithm(), ocflPropsConfig.isOcflUpgradeOnWrite(),
                    ocflPropsConfig.verifyInventory());
        }
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public OcflObjectSessionFactory ocflObjectSessionFactory() throws IOException {
        final var objectMapper = OcflPersistentStorageUtils.objectMapper();

        final var factory = new DefaultOcflObjectSessionFactory(repository(),
                ocflPropsConfig.getFedoraOcflStaging(),
                objectMapper,
                createCache("resourceHeadersCache"),
                createCache("rootIdCache"),
                commitType(),
                "Authored by Fedora 6",
                "fedoraAdmin",
                "info:fedora/fedoraAdmin");
        factory.useUnsafeWrite(ocflPropsConfig.isUnsafeWriteEnabled());
        return factory;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ObjectValidator objectValidator() throws IOException {
        final var objectMapper = OcflPersistentStorageUtils.objectMapper();
        return new ObjectValidator(repository(), objectMapper.readerFor(ResourceHeaders.class));
    }

    private CommitType commitType() {
        if (ocflPropsConfig.isAutoVersioningEnabled()) {
            return CommitType.NEW_VERSION;
        }
        return CommitType.UNVERSIONED;
    }

    private S3AsyncClient s3CrtClient() {
        final var builder = S3AsyncClient.crtBuilder()
                .checksumValidationEnabled(ocflPropsConfig.isOcflS3ChecksumEnabled());

        if (StringUtils.isNotBlank(ocflPropsConfig.getAwsRegion())) {
            builder.region(Region.of(ocflPropsConfig.getAwsRegion()));
        }

        if (StringUtils.isNotBlank(ocflPropsConfig.getS3Endpoint())) {
            builder.endpointOverride(URI.create(ocflPropsConfig.getS3Endpoint()));
        }

        if (ocflPropsConfig.isPathStyleAccessEnabled()) {
            builder.forcePathStyle(true);
        }

        if (StringUtils.isNoneBlank(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())));
        }

        return builder.build();
    }

    private S3AsyncClient s3Client() {
        final var builder = S3AsyncClient.builder();

        if (StringUtils.isNotBlank(ocflPropsConfig.getAwsRegion())) {
            builder.region(Region.of(ocflPropsConfig.getAwsRegion()));
        }

        if (StringUtils.isNotBlank(ocflPropsConfig.getS3Endpoint())) {
            builder.endpointOverride(URI.create(ocflPropsConfig.getS3Endpoint()));
        }

        if (ocflPropsConfig.isPathStyleAccessEnabled()) {
            builder.serviceConfiguration(config -> config.pathStyleAccessEnabled(true));
        }

        if (StringUtils.isNoneBlank(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())));
        }

        // May want to do additional HTTP client configuration, connection pool, etc
        final var httpClientBuilder = NettyNioAsyncHttpClient.builder()
                .connectionAcquisitionTimeout(Duration.ofSeconds(ocflPropsConfig.getS3ConnectionTimeout()))
                .writeTimeout(Duration.ofSeconds(ocflPropsConfig.getS3WriteTimeout()))
                .readTimeout(Duration.ofSeconds(ocflPropsConfig.getS3ReadTimeout()))
                .maxConcurrency(ocflPropsConfig.getS3MaxConcurrency());
        builder.httpClientBuilder(httpClientBuilder);

        return builder.build();
    }

    private <K, V> Cache<K, V> createCache(final String metricName) {
        if (ocflPropsConfig.isResourceHeadersCacheEnabled()) {
            final var builder = Caffeine.newBuilder();

            if (metricsConfig.isMetricsEnabled()) {
                builder.recordStats();
            }

            final var cache = builder
                    .maximumSize(ocflPropsConfig.getResourceHeadersCacheMaxSize())
                    .expireAfterAccess(ocflPropsConfig.getResourceHeadersCacheExpireAfterSeconds(), TimeUnit.SECONDS)
                    .build();

            if (metricsConfig.isMetricsEnabled()) {
                CaffeineCacheMetrics.monitor(meterRegistry, cache, metricName);
            }

            return new CaffeineCache<>(cache);
        }

        return new NoOpCache<>();
    }

}
