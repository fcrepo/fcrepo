/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.persistence.ocfl.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.config.MetricsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.config.Storage;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.cache.Cache;
import org.fcrepo.storage.ocfl.cache.CaffeineCache;
import org.fcrepo.storage.ocfl.cache.NoOpCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createS3Repository;

/**
 * A Configuration for OCFL dependencies
 *
 * @author dbernstein
 * @since 6.0.0
 */

@Configuration
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
    public MutableOcflRepository repository() throws IOException {
        if (ocflPropsConfig.getStorage() == Storage.OCFL_S3) {
            return createS3Repository(
                    dataSource,
                    s3Client(),
                    ocflPropsConfig.getOcflS3Bucket(),
                    ocflPropsConfig.getOcflS3Prefix(),
                    ocflPropsConfig.getOcflTemp());
        } else {
            return createFilesystemRepository(ocflPropsConfig.getOcflRepoRoot(), ocflPropsConfig.getOcflTemp());
        }
    }

    @Bean
    public OcflObjectSessionFactory ocflObjectSessionFactory() throws IOException {
        final var objectMapper = OcflPersistentStorageUtils.objectMapper();

        return new DefaultOcflObjectSessionFactory(repository(),
                ocflPropsConfig.getFedoraOcflStaging(),
                objectMapper,
                resourceHeadersCache(),
                commitType(),
                "Authored by Fedora 6",
                "fedoraAdmin",
                "info:fedora/fedoraAdmin");
    }

    private CommitType commitType() {
        if (ocflPropsConfig.isAutoVersioningEnabled()) {
            return CommitType.NEW_VERSION;
        }
        return CommitType.UNVERSIONED;
    }

    private S3Client s3Client() {
        final var builder = S3Client.builder();

        if (StringUtils.isNotBlank(ocflPropsConfig.getAwsRegion())) {
            builder.region(Region.of(ocflPropsConfig.getAwsRegion()));
        }

        if (StringUtils.isNoneBlank(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ocflPropsConfig.getAwsAccessKey(), ocflPropsConfig.getAwsSecretKey())));
        }

        // May want to do additional HTTP client configuration, connection pool, etc

        return builder.build();
    }

    private Cache<String, ResourceHeaders> resourceHeadersCache() {
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
                CaffeineCacheMetrics.monitor(meterRegistry, cache, "resourceHeadersCache");
            }

            return new CaffeineCache<>(cache);
        }

        return new NoOpCache<>();
    }

}