/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ocfl.api.DigestAlgorithmRegistry;
import io.ocfl.api.MutableOcflRepository;
import io.ocfl.api.model.DigestAlgorithm;
import io.ocfl.api.model.OcflVersion;
import io.ocfl.aws.OcflS3Client;
import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.extension.storage.layout.config.HashedNTupleLayoutConfig;
import io.ocfl.core.path.constraint.ContentPathConstraints;
import io.ocfl.core.path.mapper.LogicalPathMappers;
import io.ocfl.core.storage.OcflStorageBuilder;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.impl.auth.UnsupportedDigestAlgorithmException;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * A set of utility functions for supporting OCFL persistence activities.
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class OcflPersistentStorageUtils {

    private static final Logger log = LoggerFactory.getLogger(OcflPersistentStorageUtils.class);

    private OcflPersistentStorageUtils() {
    }

    /**
     * The version of OCFL the repository is configured to use.
     */
    private static final OcflVersion OCFL_VERSION = OcflVersion.OCFL_1_1;

    /**
     * The default RDF on disk format
     * TODO Make this value configurable
     */
    private static RDFFormat DEFAULT_RDF_FORMAT = NTRIPLES;

    /**
     * @return the RDF Format. By default NTRIPLES are returned.
     */
    public static RDFFormat getRdfFormat() {
        return DEFAULT_RDF_FORMAT;
    }

    /**
     * @return the RDF file extension.
     */
    public static String getRDFFileExtension() {
        return "." + DEFAULT_RDF_FORMAT.getLang().getFileExtensions().get(0);
    }

    /**
     * Create a new ocfl repository backed by the filesystem
     * @param ocflStorageRootDir The ocfl storage root directory
     * @param ocflWorkDir The ocfl work directory
     * @param algorithm the algorithm for the OCFL repository
     * @param ocflUpgradeOnWrite true if we want to write new versions on older objects.
     * @param verifyInventory true if we should verify the inventory
     * @return the repository
     */
    public static MutableOcflRepository createFilesystemRepository(final Path ocflStorageRootDir,
                                                                   final Path ocflWorkDir,
                                                                   final org.fcrepo.config.DigestAlgorithm algorithm,
                                                                   final boolean ocflUpgradeOnWrite,
                                                                   final boolean verifyInventory)
            throws IOException {
        createDirectories(ocflStorageRootDir);

        final var storage = OcflStorageBuilder.builder()
                                              .verifyInventoryDigest(verifyInventory)
                                              .fileSystem(ocflStorageRootDir).build();

        return createRepository(ocflWorkDir, builder -> {
            builder.storage(storage);
        }, algorithm, ocflUpgradeOnWrite);
    }

    /**
     * Create a new ocfl repository backed by s3
     *
     * @param dataSource the datasource to keep inventories in and use as a lock
     * @param s3Client aws s3 client
     * @param bucket the bucket to store objects in
     * @param prefix the prefix within the bucket to store objects under
     * @param ocflWorkDir the local directory to stage objects in
     * @param algorithm the algorithm for the OCFL repository
     * @param withDb true if the ocfl client should use a db
     * @param ocflUpgradeOnWrite true if we want to write new versions on older objects.
     * @param verifyInventory true if we should verify the ocfl inventory
     * @return the repository
     */
    public static MutableOcflRepository createS3Repository(final DataSource dataSource,
                                                           final S3Client s3Client,
                                                           final String bucket,
                                                           final String prefix,
                                                           final Path ocflWorkDir,
                                                           final org.fcrepo.config.DigestAlgorithm algorithm,
                                                           final boolean withDb,
                                                           final boolean ocflUpgradeOnWrite,
                                                           final boolean verifyInventory)
            throws IOException {
        createDirectories(ocflWorkDir);

        final var storage = OcflStorageBuilder.builder()
            .verifyInventoryDigest(verifyInventory)
            .cloud(OcflS3Client.builder()
                .s3Client(s3Client)
                .bucket(bucket)
                .repoPrefix(prefix)
                .build())
                .build();

        return createRepository(ocflWorkDir, builder -> {
            builder.contentPathConstraints(ContentPathConstraints.cloud())
                    .storage(storage);

            if (withDb) {
                builder.objectDetailsDb(db -> db.dataSource(dataSource));
            }

        }, algorithm, ocflUpgradeOnWrite);
    }

    private static MutableOcflRepository createRepository(final Path ocflWorkDir,
                                                          final Consumer<OcflRepositoryBuilder> configurer,
                                                          final org.fcrepo.config.DigestAlgorithm algorithm,
                                                          final boolean ocflUpgradeOnWrite)
            throws IOException {
        createDirectories(ocflWorkDir);

        final DigestAlgorithm ocflDigestAlg = translateFedoraDigestToOcfl(algorithm);
        if (ocflDigestAlg == null) {
            throw new UnsupportedDigestAlgorithmException(
                    "Unable to map Fedora default digest algorithm " + algorithm + " into OCFL");
        }

        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        final var builder = new OcflRepositoryBuilder()
                .defaultLayoutConfig(new HashedNTupleLayoutConfig())
                .ocflConfig(config -> config.setDefaultDigestAlgorithm(ocflDigestAlg)
                        .setOcflVersion(OCFL_VERSION)
                        .setUpgradeObjectsOnWrite(ocflUpgradeOnWrite))
                .logicalPathMapper(logicalPathMapper)
                .workDir(ocflWorkDir);

        configurer.accept(builder);

        return builder.buildMutable();
    }

    /**
     * @return new object mapper with default config
     */
    public static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Translates the provided fedora digest algorithm enum into a OCFL client digest algorithm
     *
     * @param fcrepoAlg fedora digest algorithm
     * @return OCFL client DigestAlgorithm, or null if no match could be made
     */
    public static DigestAlgorithm translateFedoraDigestToOcfl(final org.fcrepo.config.DigestAlgorithm fcrepoAlg) {
        return fcrepoAlg.getAliases().stream()
                .map(alias -> DigestAlgorithmRegistry.getAlgorithm(alias))
                .filter(alg -> alg != null)
                .findFirst()
                .orElse(null);
    }

    private static Path createDirectories(final Path path) throws IOException {
        try {
            return Files.createDirectories(path);
        } catch (final FileAlreadyExistsException e) {
            // Ignore. This only happens with the path is a symlink
            return path;
        }
    }
}
