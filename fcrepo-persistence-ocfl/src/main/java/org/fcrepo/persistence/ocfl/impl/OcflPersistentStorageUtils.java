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

import edu.wisc.library.ocfl.api.DigestAlgorithmRegistry;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;
import edu.wisc.library.ocfl.core.OcflConfig;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedTruncatedNTupleConfig;
import edu.wisc.library.ocfl.core.path.mapper.LogicalPathMappers;
import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.impl.auth.UnsupportedDigestAlgorithmException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.fcrepo.persistence.ocfl.api.OcflVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;

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
     * Returns the RDF topic to be returned for a given resource identifier
     * For example:  passing info:fedora/resource1/fcr:metadata would return
     *  info:fedora/resource1 since  info:fedora/resource1 would be the expected
     *  topic.
     * @param fedoraIdentifier The fedora identifier
     * @return The resolved topic
     */
    private static FedoraId resolveTopic(final FedoraId fedoraIdentifier) {
        if (fedoraIdentifier.isDescription()) {
            return fedoraIdentifier.asBaseId();
        } else {
            return fedoraIdentifier;
        }
    }

    /**
     * Writes an RDFStream to a contentPath within an ocfl object.
     *
     * @param session The object session
     * @param triples The triples
     * @param contentPath The contentPath within the OCFL Object
     * @return the outcome of the write operation
     * @throws PersistentStorageException on write failure
     */
    public static WriteOutcome writeRDF(final OcflObjectSession session,
                                        final RdfStream triples, final String contentPath)
            throws PersistentStorageException {
        try (final var os = new ByteArrayOutputStream()) {
            final StreamRDF streamRDF = getWriterStream(os, getRdfFormat());
            streamRDF.start();
            if (triples != null) {
                triples.forEach(streamRDF::triple);
            }
            streamRDF.finish();

            final var is = new ByteArrayInputStream(os.toByteArray());
            final var outcome = session.write(contentPath, is);
            log.debug("wrote {} to {}", contentPath, session);
            return outcome;
        } catch (final IOException ex) {
            throw new PersistentStorageException(
                    format("failed to write contentPath %s in %s", contentPath, session), ex);
        }
    }

    private static InputStream readFile(final OcflObjectSession objSession, final String subpath, final String version)
            throws PersistentStorageException {
        return version == null ? objSession.read(subpath) : objSession.read(subpath, version);
    }

    /**
     * Get the content of the specified binary file.
     *
     * @param objSession The OCFL object session
     * @param subpath The path to the desired file
     * @param version The version. If null, the head state will be returned.
     * @return the binary content stream
     * @throws PersistentStorageException If unable to read the specified binary stream.
     */
    public static InputStream getBinaryStream(final OcflObjectSession objSession,
            final String subpath, final Instant version) throws PersistentStorageException {
        final String versionId = resolveVersionId(objSession, version);
        return readFile(objSession, subpath, versionId);
    }

    /**
     * Get an RDF stream for the specified file.
     *
     * @param identifier The resource identifier
     * @param version    The version.  If null, the head state will be returned.
     * @param objSession The OCFL object session
     * @param subpath The path to the desired file.
     * @return the RDF stream
     * @throws PersistentStorageException If unable to read the specified rdf stream.
     */
    public static RdfStream getRdfStream(final FedoraId identifier,
                                         final OcflObjectSession objSession,
                                         final String subpath,
                                         final Instant version) throws PersistentStorageException {
        final String versionId = resolveVersionId(objSession, version);
        try (final InputStream is = readFile(objSession, subpath, versionId)) {
            final Model model = createDefaultModel();
            RDFDataMgr.read(model, is, DEFAULT_RDF_FORMAT.getLang());
            final FedoraId topic = resolveTopic(identifier);
            return DefaultRdfStream.fromModel(createURI(topic.getFullId()), model);
        } catch (final IOException ex) {
            throw new PersistentStorageException(format("unable to read %s ;  version = %s", identifier, version), ex);
        }
    }

    /**
     * Resolve an instant to a version
     *
     * @param objSession session
     * @param version version time
     * @return name of version
     * @throws PersistentStorageException thrown if version not found
     */
    public static String resolveVersionId(final OcflObjectSession objSession, final Instant version)
            throws PersistentStorageException {
        if (version != null) {
            final var versions = objSession.listVersions();
            // reverse order so that the most recent version is matched first
            Collections.reverse(versions);
            return versions.stream()
                    .filter(vd -> {
                        return vd.getCreated().equals(version);
                    }).map(OcflVersion::getOcflVersionId)
                    .findFirst()
                    .orElseThrow(() -> {
                        //otherwise throw an exception.
                        return new PersistentItemNotFoundException(format(
                                "There is no version in %s with a created date matchin %s",
                                objSession, version));
                    });
        } else {
            //return null if the instant is null
            return null;
        }
    }

    /**
     * A utility method that returns a list of  {@link java.time.Instant} objects representing immutable versions
     * accessible from the OCFL Object represented by the session.
     *
     * @param objSession The OCFL object session
     * @param subpath The subpath within the object to list versions of; if blank all of the versions of the object are
     *                listed
     * @return A list of Instant objects
     * @throws PersistentStorageException On read failure due to the session being closed or some other problem.
     */
    public static List<Instant> listVersions(final OcflObjectSession objSession, final String subpath)
            throws PersistentStorageException {
        return  objSession.listVersions(subpath).stream()
                .map(OcflVersion::getCreated)
                .collect(Collectors.toList());
    }

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
     * Create a new ocfl repository
     * @param ocflStorageRootDir The ocfl storage root directory
     * @param ocflWorkDir The ocfl work directory
     * @return the repository
     */
    public static MutableOcflRepository createRepository(final Path ocflStorageRootDir, final Path ocflWorkDir)
            throws IOException {
        Files.createDirectories(ocflStorageRootDir);
        Files.createDirectories(ocflWorkDir);

        log.debug("Fedora OCFL persistence directories:\n- {}\n- {}", ocflStorageRootDir, ocflWorkDir);
        final var defaultFcrepoAlg = ContentDigest.DEFAULT_DIGEST_ALGORITHM;
        final DigestAlgorithm ocflDigestAlg = translateFedoraDigestToOcfl(defaultFcrepoAlg);
        if (ocflDigestAlg == null) {
            throw new UnsupportedDigestAlgorithmException(
                    "Unable to map Fedora default digest algorithm " + defaultFcrepoAlg + " into OCFL");
        }

        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        return new OcflRepositoryBuilder()
                .layoutConfig(new HashedTruncatedNTupleConfig())
                .ocflConfig(new OcflConfig().setDefaultDigestAlgorithm(ocflDigestAlg))
                .logicalPathMapper(logicalPathMapper)
                .storage((FileSystemOcflStorage.builder().repositoryRoot(ocflStorageRootDir).build()))
                .workDir(ocflWorkDir)
                .buildMutable();
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
