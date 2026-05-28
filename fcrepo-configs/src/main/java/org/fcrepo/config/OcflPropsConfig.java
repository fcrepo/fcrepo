/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Fedora's OCFL related configuration properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class OcflPropsConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPropsConfig.class);

    public static final String FCREPO_OCFL_STAGING = "fcrepo.ocfl.staging";
    public static final String FCREPO_OCFL_ROOT = "fcrepo.ocfl.root";
    public static final String FCREPO_OCFL_TEMP = "fcrepo.ocfl.temp";
    private static final String FCREPO_OCFL_S3_BUCKET = "fcrepo.ocfl.s3.bucket";

    private static final String OCFL_STAGING = "staging";
    private static final String OCFL_ROOT = "ocfl-root";
    private static final String OCFL_TEMP = "ocfl-temp";

    private static final String FCREPO_PERSISTENCE_ALGORITHM = "fcrepo.persistence.defaultDigestAlgorithm";

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

    @Value("${fcrepo.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${fcrepo.s3.path.style.access:false}")
    private boolean pathStyleAccessEnabled;

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

    @Value("${" + FCREPO_PERSISTENCE_ALGORITHM + ":sha512}")
    private String FCREPO_DIGEST_ALGORITHM_VALUE;

    @Value("${fcrepo.ocfl.s3.db.enabled:true}")
    private boolean ocflS3DbEnabled;

    @Value("${fcrepo.ocfl.unsafe.write.enabled:false}")
    private boolean unsafeWriteEnabled;

    @Value("${fcrepo.cache.db.ocfl.id_map.size.entries:1024}")
    private long fedoraToOcflCacheSize;

    @Value("${fcrepo.cache.db.ocfl.id_map.timeout.minutes:30}")
    private long fedoraToOcflCacheTimeout;

    @Value("${fcrepo.ocfl.upgrade.enabled:false}")
    private boolean ocflUpgradeOnWrite;

    @Value("${fcrepo.ocfl.verify.inventory:true}")
    private boolean verifyInventory;

    @Value("${fcrepo.ocfl.s3.timeout.connection.seconds:60}")
    private int s3ConnectionTimeout;

    @Value("${fcrepo.ocfl.s3.timeout.write.seconds:60}")
    private int s3WriteTimeout;

    @Value("${fcrepo.ocfl.s3.timeout.read.seconds:60}")
    private int s3ReadTimeout;

    @Value("${fcrepo.ocfl.s3.max_concurrency:100}")
    private int s3MaxConcurrency;

    @Value("${fcrepo.ocfl.s3.enable.checksum:true}")
    private boolean s3EnableChecksum;

    @Value("${fcrepo.ocfl.show_path:false}")
    private boolean showPath;

    private DigestAlgorithm FCREPO_DIGEST_ALGORITHM;

    /**
     * List of valid choices for fcrepo.persistence.defaultDigestAlgorithm
     */
    private static final List<DigestAlgorithm> FCREPO_VALID_DIGEST_ALGORITHMS = List.of(
            DigestAlgorithm.SHA256,
            DigestAlgorithm.SHA512
    );

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
        createDirectories(fedoraOcflStaging);
        createDirectories(ocflTemp);

        if (storage == Storage.OCFL_FILESYSTEM) {
            LOGGER.info("Fedora OCFL root: {}", ocflRepoRoot);
            createDirectories(ocflRepoRoot);
        } else if (storage == Storage.OCFL_S3) {
            if (ocflS3Bucket == null || ocflS3Bucket.isBlank()) {
                throw new IllegalArgumentException(
                        String.format("The property %s must be set when OCFL S3 storage is used",
                                FCREPO_OCFL_S3_BUCKET));
            }

            LOGGER.info("Fedora AWS access key: {}", awsAccessKey);
            LOGGER.info("Fedora AWS secret key set: {}", Objects.isNull(awsSecretKey));
            LOGGER.info("Fedora AWS region: {}", awsRegion);
            LOGGER.info("Fedora OCFL S3 bucket: {}", ocflS3Bucket);
            LOGGER.info("Fedora OCFL S3 prefix: {}", ocflS3Prefix);
        }
        FCREPO_DIGEST_ALGORITHM = DigestAlgorithm.fromAlgorithm(FCREPO_DIGEST_ALGORITHM_VALUE);
        // Throw error if the configured default digest is not known to fedora or is not a valid option
        if (DigestAlgorithm.MISSING.equals(FCREPO_DIGEST_ALGORITHM) ||
                !FCREPO_VALID_DIGEST_ALGORITHMS.contains(FCREPO_DIGEST_ALGORITHM)) {
            throw new IllegalArgumentException(String.format("Invalid %s property configured: %s, must be one of %s",
                    FCREPO_PERSISTENCE_ALGORITHM, FCREPO_DIGEST_ALGORITHM_VALUE,
                    FCREPO_VALID_DIGEST_ALGORITHMS.stream().map(DigestAlgorithm::getAlgorithm)
                            .collect(Collectors.joining(", "))));
        }
        LOGGER.info("Fedora OCFL digest algorithm: {}", FCREPO_DIGEST_ALGORITHM.getAlgorithm());
        LOGGER.info("Fedora OCFL show path: {}", showPath);
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
        return Math.max(availableThreads - 1, 1);
    }

    /**
     * @return the configured OCFL digest algorithm
     */
    public DigestAlgorithm getDefaultDigestAlgorithm() {
        return FCREPO_DIGEST_ALGORITHM;
    }

    /**
     * @return an optional custom s3 endpoint or null
     */
    public String getS3Endpoint() {
        return s3Endpoint;
    }

    /**
     * @return true if path style S3 access should be used
     */
    public boolean isPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    /**
     * @return true if the ocfl client should be configured to use a database when storing objects in S3
     */
    public boolean isOcflS3DbEnabled() {
        return ocflS3DbEnabled;
    }

    /**
     * When unsafe writes are enabled, the OCFL client does not calculate a digest for files that are added, and
     * trusts the digest value that it's given. If this value is incorrect, the object will be corrupted.
     *
     * @return true if objects should be written to OCFL using an "unsafe" write
     */
    public boolean isUnsafeWriteEnabled() {
        return unsafeWriteEnabled;
    }

    /**
     * @return Size of the fedoraToOcflIndex cache.
     */
    public long getFedoraToOcflCacheSize() {
        return fedoraToOcflCacheSize;
    }

    /**
     * @return Time to cache expiration in minutes.
     */
    public long getFedoraToOcflCacheTimeout() {
        return fedoraToOcflCacheTimeout;
    }

    /**
     * @return True to write new versions of OCFL on older objects, false to keep the original version.
     */
    public boolean isOcflUpgradeOnWrite() {
        return ocflUpgradeOnWrite;
    }

    /**
     * @return True to verify inventory when performing OCFL operations
     */
    public boolean verifyInventory() {
        return verifyInventory;
    }

    /**
     * @return True if client checksums should be used when writing using S3 client.
     */
    public boolean isOcflS3ChecksumEnabled() {
        return s3EnableChecksum;
    }

    /**
     * @return the AWS S3 connection acquisition timeout in seconds.
     */
    public int getS3ConnectionTimeout() {
        return s3ConnectionTimeout;
    }

    /**
     * @return the AWS S3 write timeout in seconds.
     */
    public int getS3WriteTimeout() {
        return s3WriteTimeout;
    }

    /**
     * @return the AWS S3 read timeout in seconds.
     */
    public int getS3ReadTimeout() {
        return s3ReadTimeout;
    }

    /**
     * @return the maximum number of concurrent S3 operations.
     */
    public int getS3MaxConcurrency() {
        return s3MaxConcurrency;
    }

    /**
     * @return true if the path of the OCFL object should be shown
     */
    public boolean isShowPath() {
        return showPath;
    }
}
