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

package org.fcrepo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Fedora's OCFL related configuration properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
public class OcflPropsConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPropsConfig.class);

    public static final String FCREPO_OCFL_STAGING = "fcrepo.ocfl.staging";
    public static final String FCREPO_OCFL_ROOT = "fcrepo.ocfl.root";
    public static final String FCREPO_OCFL_TEMP = "fcrepo.ocfl.temp";
    private static final String FCREPO_OCFL_S3_BUCKET = "fcrepo.ocfl.s3.bucket";

    private static final String OCFL_STAGING = "staging";
    private static final String OCFL_ROOT = "ocfl-root";
    private static final String OCFL_TEMP = "ocfl-temp";

    @Value("${" + FCREPO_OCFL_STAGING + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_STAGING + "')}}")
    private Path fedoraOcflStaging;

    @Value("${" + FCREPO_OCFL_ROOT + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_ROOT + "')}}")
    private Path ocflRepoRoot;

    @Value("${" + FCREPO_OCFL_TEMP + ":#{fedoraPropsConfig.fedoraData.resolve('" + OCFL_TEMP + "')}}")
    private Path ocflTemp;

    /**
     * Controls whether changes are committed to new OCFL versions or to a mutable HEAD
     */
    @Value("${fcrepo.autoversioning.enabled:true}")
    private boolean autoVersioningEnabled;

    @Value("${fcrepo.storage:ocfl-fs}")
    private String storageStr;
    private Storage storage;

    @Value("${fcrepo.aws.access-key:}")
    private String awsAccessKey;

    @Value("${fcrepo.aws.secret-key:}")
    private String awsSecretKey;

    @Value("${fcrepo.aws.region:}")
    private String awsRegion;

    @Value("${" + FCREPO_OCFL_S3_BUCKET + ":}")
    private String ocflS3Bucket;

    @Value("${fcrepo.ocfl.s3.prefix:}")
    private String ocflS3Prefix;

    @Value("${fcrepo.resource-header-cache.enable:true}")
    private boolean resourceHeadersCacheEnabled;

    @Value("${fcrepo.resource-header-cache.max-size:512}")
    private long resourceHeadersCacheMaxSize;

    @Value("${fcrepo.resource-header-cache.expire-after-seconds:600}")
    private long resourceHeadersCacheExpireAfterSeconds;

    @Value("${fcrepo.ocfl.reindex.threads:-1}")
    private long reindexThreads;

    @Value("${fcrepo.ocfl.reindex.batchSize:100}")
    private long reindexBatchSize;

    @Value("${fcrepo.ocfl.reindex.failOnError:true}")
    private boolean reindexFailOnError;

    private static final long availableThreads = Runtime.getRuntime().availableProcessors();

    @PostConstruct
    private void postConstruct() throws IOException {
        if (reindexThreads < 0L) {
            reindexThreads = computeDefaultReindexThreads();
        } else {
            reindexThreads = checkReindexThreadLimit(reindexThreads);
        }
        storage = Storage.fromString(storageStr);
        LOGGER.info("Fedora storage type: {}", storage);
        LOGGER.info("Fedora staging: {}", fedoraOcflStaging);
        LOGGER.info("Fedora OCFL temp: {}", ocflTemp);
        LOGGER.info("Fedora OCFL reindexing threads: {}", reindexThreads);
        LOGGER.info("Fedora OCFL reindexing batch size: {}", reindexBatchSize);
        LOGGER.info("Fedora OCFL reindexing fail on error: {}", reindexFailOnError);
        Files.createDirectories(fedoraOcflStaging);
        Files.createDirectories(ocflTemp);

        if (storage == Storage.OCFL_FILESYSTEM) {
            LOGGER.info("Fedora OCFL root: {}", ocflRepoRoot);
            Files.createDirectories(ocflRepoRoot);
        } else if (storage == Storage.OCFL_S3) {
            Objects.requireNonNull(ocflS3Bucket,
                    String.format("The property %s must be set when OCFL S3 storage is used", FCREPO_OCFL_S3_BUCKET));

            LOGGER.info("Fedora AWS access key: {}", awsAccessKey);
            LOGGER.info("Fedora AWS secret key set: {}", Objects.isNull(awsSecretKey));
            LOGGER.info("Fedora AWS region: {}", awsRegion);
            LOGGER.info("Fedora OCFL S3 bucket: {}", ocflS3Bucket);
            LOGGER.info("Fedora OCFL S3 prefix: {}", ocflS3Prefix);
        }
    }

    /**
     * @return Path to directory Fedora stages resources before moving them into OCFL
     */
    public Path getFedoraOcflStaging() {
        return fedoraOcflStaging;
    }

    /**
     * Sets the path to the Fedora staging directory -- should only be used for testing purposes.
     *
     * @param fedoraOcflStaging Path to Fedora staging directory
     */
    public void setFedoraOcflStaging(final Path fedoraOcflStaging) {
        this.fedoraOcflStaging = fedoraOcflStaging;
    }

    /**
     * @return Path to OCFL root directory
     */
    public Path getOcflRepoRoot() {
        return ocflRepoRoot;
    }

    /**
     * Sets the path to the Fedora OCFL root directory -- should only be used for testing purposes.
     *
     * @param ocflRepoRoot Path to Fedora OCFL root directory
     */
    public void setOcflRepoRoot(final Path ocflRepoRoot) {
        this.ocflRepoRoot = ocflRepoRoot;
    }

    /**
     * @return Path to the temp directory used by the OCFL client
     */
    public Path getOcflTemp() {
        return ocflTemp;
    }

    /**
     * Sets the path to the OCFL temp directory -- should only be used for testing purposes.
     *
     * @param ocflTemp Path to OCFL temp directory
     */
    public void setOcflTemp(final Path ocflTemp) {
        this.ocflTemp = ocflTemp;
    }

    /**
     * @return true if every update should create a new OCFL version; false if the mutable HEAD should be used
     */
    public boolean isAutoVersioningEnabled() {
        return autoVersioningEnabled;
    }

    /**
     * Determines whether or not new OCFL versions are created on every update.
     *
     * @param autoVersioningEnabled true to create new versions on every update
     */
    public void setAutoVersioningEnabled(final boolean autoVersioningEnabled) {
        this.autoVersioningEnabled = autoVersioningEnabled;
    }

    /**
     * @return Indicates the storage type. ocfl-fs is the default
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * @param storage storage to use
     */
    public void setStorage(final Storage storage) {
        this.storage = storage;
    }

    /**
     * @return the aws access key to use, may be null
     */
    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    /**
     * @param awsAccessKey the aws access key to use
     */
    public void setAwsAccessKey(final String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    /**
     * @return the aws secret key to use, may be null
     */
    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    /**
     * @param awsSecretKey the aws secret key to use
     */
    public void setAwsSecretKey(final String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    /**
     * @return the aws region to use, may be null
     */
    public String getAwsRegion() {
        return awsRegion;
    }

    /**
     * @param awsRegion the aws region to use
     */
    public void setAwsRegion(final String awsRegion) {
        this.awsRegion = awsRegion;
    }

    /**
     * @return the s3 bucket to store objects in
     */
    public String getOcflS3Bucket() {
        return ocflS3Bucket;
    }

    /**
     * @param ocflS3Bucket sets the s3 bucket to store objects in
     */
    public void setOcflS3Bucket(final String ocflS3Bucket) {
        this.ocflS3Bucket = ocflS3Bucket;
    }

    /**
     * @return the s3 prefix to store objects under, may be null
     */
    public String getOcflS3Prefix() {
        return ocflS3Prefix;
    }

    /**
     * @param ocflS3Prefix the prefix to store objects under
     */
    public void setOcflS3Prefix(final String ocflS3Prefix) {
        this.ocflS3Prefix = ocflS3Prefix;
    }

    /**
     * @return whether or not to enable the resource headers cache
     */
    public boolean isResourceHeadersCacheEnabled() {
        return resourceHeadersCacheEnabled;
    }

    /**
     * @param resourceHeadersCacheEnabled whether or not to enable the resource headers cache
     */
    public void setResourceHeadersCacheEnabled(final boolean resourceHeadersCacheEnabled) {
        this.resourceHeadersCacheEnabled = resourceHeadersCacheEnabled;
    }

    /**
     * @return maximum number or resource headers in cache
     */
    public long getResourceHeadersCacheMaxSize() {
        return resourceHeadersCacheMaxSize;
    }

    /**
     * @param resourceHeadersCacheMaxSize maximum number of resource headers in cache
     */
    public void setResourceHeadersCacheMaxSize(final long resourceHeadersCacheMaxSize) {
        this.resourceHeadersCacheMaxSize = resourceHeadersCacheMaxSize;
    }

    /**
     * @return number of seconds to wait before expiring a resource header from the cache
     */
    public long getResourceHeadersCacheExpireAfterSeconds() {
        return resourceHeadersCacheExpireAfterSeconds;
    }

    /**
     * @param resourceHeadersCacheExpireAfterSeconds
     *      number of seconds to wait before expiring a resource header from the cache
     */
    public void setResourceHeadersCacheExpireAfterSeconds(final long resourceHeadersCacheExpireAfterSeconds) {
        this.resourceHeadersCacheExpireAfterSeconds = resourceHeadersCacheExpireAfterSeconds;
    }

    /**
     * @param threads
     *   number of threads to use when rebuilding from Fedora OCFL on disk.
     */
    public void setReindexingThreads(final long threads) {
        this.reindexThreads = checkReindexThreadLimit(threads);
    }

    /**
     * @return number of threads to use when rebuilding from Fedora OCFL on disk.
     */
    public long getReindexingThreads() {
        return this.reindexThreads;
    }

    /**
     * @return number of OCFL ids for a the reindexing manager to hand out at once.
     */
    public long getReindexBatchSize() {
        return reindexBatchSize;
    }

    /**
     * @param reindexBatchSize
     *   number of OCFL ids for a the reindexing manager to hand out at once.
     */
    public void setReindexBatchSize(final long reindexBatchSize) {
        this.reindexBatchSize = reindexBatchSize;
    }

    /**
     * @return whether to stop the entire reindexing process if a single object fails.
     */
    public boolean isReindexFailOnError() {
        return reindexFailOnError;
    }

    /**
     * @param reindexFailOnError
     *   whether to stop the entire reindexing process if a single object fails.
     */
    public void setReindexFailOnError(final boolean reindexFailOnError) {
        this.reindexFailOnError = reindexFailOnError;
    }

    /**
     * Check we don't create too few reindexing threads.
     * @param threads the number of threads requested.
     * @return higher of the requested amount or 1
     */
    private long checkReindexThreadLimit(final long threads) {
       if (threads <= 0) {
            LOGGER.warn("Can't have fewer than 1 reindexing thread, setting to 1.");
            return 1;
        } else {
            return threads;
        }
    }

    /**
     * @return number of available processors minus 1.
     */
    private static long computeDefaultReindexThreads() {
        return availableThreads - 1;
    }
}
