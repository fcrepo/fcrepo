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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.wisc.library.ocfl.api.DigestAlgorithmRegistry;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflConfig;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;
import edu.wisc.library.ocfl.aws.OcflS3Client;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedTruncatedNTupleConfig;
import edu.wisc.library.ocfl.core.path.constraint.ContentPathConstraints;
import edu.wisc.library.ocfl.core.path.mapper.LogicalPathMappers;
import edu.wisc.library.ocfl.core.storage.cloud.CloudOcflStorage;
import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.impl.auth.UnsupportedDigestAlgorithmException;
import org.apache.jena.riot.RDFFormat;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;

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
     * @return the repository
     */
    public static MutableOcflRepository createFilesystemRepository(final Path ocflStorageRootDir,
                                                                   final Path ocflWorkDir) throws IOException {
        Files.createDirectories(ocflStorageRootDir);

        final var storage = FileSystemOcflStorage.builder().repositoryRoot(ocflStorageRootDir).build();

        return createRepository(ocflWorkDir, builder -> {
            builder.storage(storage);
        });
    }

    /**
     * Create a new ocfl repository backed by s3
     *
     * @param dataSource the datasource to keep inventories in and use as a lock
     * @param s3Client aws s3 client
     * @param bucket the bucket to store objects in
     * @param prefix the prefix within the bucket to store objects under
     * @param ocflWorkDir the local directory to stage objects in
     * @return the repository
     */
    public static MutableOcflRepository createS3Repository(final DataSource dataSource,
                                                           final S3Client s3Client,
                                                           final String bucket,
                                                           final String prefix,
                                                           final Path ocflWorkDir) throws IOException {
        Files.createDirectories(ocflWorkDir);

        final var storage = CloudOcflStorage.builder()
                .cloudClient(OcflS3Client.builder()
                        .s3Client(s3Client)
                        .bucket(bucket)
                        .repoPrefix(prefix)
                        .build())
                .build();

        return createRepository(ocflWorkDir, builder -> {
            builder.contentPathConstraints(ContentPathConstraints.cloud())
                    .objectLock(lock -> lock.dataSource(dataSource))
                    .objectDetailsDb(db -> db.dataSource(dataSource))
                    .storage(storage);
        });
    }

    private static MutableOcflRepository createRepository(final Path ocflWorkDir,
                                                          final Consumer<OcflRepositoryBuilder> configurer)
            throws IOException {
        Files.createDirectories(ocflWorkDir);

        final var defaultFcrepoAlg = ContentDigest.DEFAULT_DIGEST_ALGORITHM;
        final DigestAlgorithm ocflDigestAlg = translateFedoraDigestToOcfl(defaultFcrepoAlg);
        if (ocflDigestAlg == null) {
            throw new UnsupportedDigestAlgorithmException(
                    "Unable to map Fedora default digest algorithm " + defaultFcrepoAlg + " into OCFL");
        }

        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        final var builder = new OcflRepositoryBuilder()
                .layoutConfig(new HashedTruncatedNTupleConfig())
                .ocflConfig(new OcflConfig().setDefaultDigestAlgorithm(ocflDigestAlg))
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
    public static DigestAlgorithm translateFedoraDigestToOcfl(final DIGEST_ALGORITHM fcrepoAlg) {
        return fcrepoAlg.getAliases().stream()
                .map(alias -> DigestAlgorithmRegistry.getAlgorithm(alias))
                .filter(alg -> alg != null)
                .findFirst()
                .orElse(null);
    }
}
