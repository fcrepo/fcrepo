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

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.layout.config.DefaultLayoutConfig;
import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.ocfl.api.OCFLPersistenceConstants.DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID;

/**
 * A set of utility functions for supporting OCFL persistence activities.
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class OCFLPersistentStorageUtils {

    private static final Logger log = LoggerFactory.getLogger(OCFLPersistentStorageUtils.class);

    private OCFLPersistentStorageUtils() {
    }

    /**
     * The directory within an OCFL Object's content directory that contains
     * information managed by Fedora.
     */
    private static final String INTERNAL_FEDORA_DIRECTORY = ".fcrepo";
    /**
     * The default RDF on disk format
     * TODO Make this value configurable
     */

    private static RDFFormat DEFAULT_RDF_FORMAT = NTRIPLES;

    private static final String FEDORA_METADATA_SUFFIX = "/" + FedoraTypes.FCR_METADATA;

    /**
     * Returns the relative subpath of the resourceId based on the root object's resource id.
     *
     * @param rootFedoraObjectId The fedora root object identifier
     * @param fedoraResourceId   The identifier of the resource whose subpath you wish to resolve.
     * @return The resolved subpath
     */
    public static String relativizeSubpath(final String rootFedoraObjectId, final String fedoraResourceId) {

        final var resourceId = trimTrailingSlashes(fedoraResourceId);
        final var rootObjectId = trimTrailingSlashes(rootFedoraObjectId);

        if (resourceId.equals(rootObjectId)) {
            return "";
        } else if (resourceId.startsWith(rootObjectId) && resourceId.charAt(rootObjectId.length()) == '/') {
            return resourceId.substring(rootObjectId.length() + 1);
        }

        throw new IllegalArgumentException(format("resource (%s) is not prefixed by root object indentifier (%s)",
                resourceId,
                rootObjectId));
    }

    private static String trimTrailingSlashes(final String string) {
        return string.replaceAll("/+$", "");
    }

    /**
     * Returns the OCFL subpath for a given fedora subpath. This returned subpath
     * does not include any added extensions.
     *
     * @param rootFedoraObjectId  The fedora object root identifier
     * @param fedoraSubpath subpath of file within ocfl object
     * @return The resolved OCFL subpath
     */
    public static  String resolveOCFLSubpath(final String rootFedoraObjectId, final String fedoraSubpath) {

        final var rootObjectId = trimTrailingSlashes(rootFedoraObjectId);

        final var lastPathSegment = rootObjectId.substring(rootObjectId.lastIndexOf("/") + 1);

        String subPath;
        if (FEDORA_ID_PREFIX.equals(rootFedoraObjectId) && "".equals(fedoraSubpath)) {
            subPath = DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID;
        } else if ("".equals(fedoraSubpath)) {
            subPath = lastPathSegment;
        } else if (fedoraSubpath.endsWith(FCR_ACL)) {
            subPath = fedoraSubpath.replaceAll("/?" + FCR_ACL + "$", "-acl");
            //prepend last path segment if acl of object root
            if (fedoraSubpath.equals(FCR_ACL)) {
                subPath = lastPathSegment + subPath;
            }
        } else if (fedoraSubpath.endsWith(FCR_METADATA)) {
            subPath = fedoraSubpath.replaceAll("/?" + FCR_METADATA + "$", "-description");
            //prepend last path segment if description of object root
            if (fedoraSubpath.equals(FCR_METADATA)) {
                subPath = lastPathSegment + subPath;
            }
        } else {
            subPath = fedoraSubpath;
        }

        return subPath;
    }

    /**
     * Returns the RDF topic to be returned for a given resource identifier
     * For example:  passing info:fedora/resource1/fcr:metadata would return
     *  info:fedora/resource1 since  info:fedora/resource1 would be the expected
     *  topic.
     * @param fedoraIdentifier The fedora identifier
     * @return The resolved topic
     */
    public static  String resolveTopic(final String fedoraIdentifier) {
        if (fedoraIdentifier.endsWith(FEDORA_METADATA_SUFFIX)) {
            return fedoraIdentifier.substring(0, fedoraIdentifier.indexOf(FEDORA_METADATA_SUFFIX));
        } else {
            return fedoraIdentifier;
        }
    }

    /**
     * Writes an RDFStream to a subpath within an ocfl object.
     *
     * @param session The object session
     * @param triples The triples
     * @param subpath The subpath within the OCFL Object
     * @return the outcome of the write operation
     * @throws PersistentStorageException on write failure
     */
    public static WriteOutcome writeRDF(final OCFLObjectSession session, final RdfStream triples, final String subpath)
            throws PersistentStorageException {
        try (final var os = new ByteArrayOutputStream()) {
            final StreamRDF streamRDF = getWriterStream(os, getRdfFormat());
            streamRDF.start();
            if (triples != null) {
                triples.forEach(streamRDF::triple);
            }
            streamRDF.finish();

            final var is = new ByteArrayInputStream(os.toByteArray());
            final var outcome = session.write(subpath + getRDFFileExtension(), is);
            log.debug("wrote {} to {}", subpath, session);
            return outcome;
        } catch (final IOException ex) {
            throw new PersistentStorageException(format("failed to write subpath %s in %s", subpath, session), ex);
        }
    }

    private static InputStream readFile(final OCFLObjectSession objSession, final String subpath, final String version)
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
    public static InputStream getBinaryStream(final OCFLObjectSession objSession,
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
    public static RdfStream getRdfStream(final String identifier,
                                         final OCFLObjectSession objSession,
                                         final String subpath,
                                         final Instant version) throws PersistentStorageException {
        final String versionId = resolveVersionId(objSession, version);
        try (final InputStream is = readFile(objSession, subpath, versionId)) {
            final Model model = createDefaultModel();
            RDFDataMgr.read(model, is, DEFAULT_RDF_FORMAT.getLang());
            final String topic = resolveTopic(identifier);
            return DefaultRdfStream.fromModel(createURI(topic), model);
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
    public static String resolveVersionId(final OCFLObjectSession objSession, final Instant version)
            throws PersistentStorageException {
        if (version != null) {
            return objSession.listVersions()
                    .stream()
                    .filter(vd -> {
                        //filter by comparing Epoch seconds since
                        //Memento has second granularity while OCFL versions are
                        //millisecond granularity
                        return vd.getCreated()
                                .toInstant()
                                .toEpochMilli() / 1000 == version.toEpochMilli() / 1000;
                    }).map(vd -> vd.getVersionId().toString()) //return the versionId the matches
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
     * Returns the subpath to the fedora metadata file associated with the specified subpath.
     * @param subpath   The subpath to the ocfl resource whose metadata file (sidecar subpath) you wish to
     *                  retrieve.
     * @return The subpath to the (sidecar) metadata file.
     */
    public static String getSidecarSubpath(final String subpath) {
        return getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION;
    }

    /**
     * Returns true of the subpath is a sidecar file
     * @param subpath The subpath to be evaluated
     * @return True if the subpath is a sidecar file.
     */
    public static boolean isSidecarSubpath(final String subpath) {
        return subpath.startsWith(getInternalFedoraDirectory()) && subpath.endsWith(RESOURCE_HEADER_EXTENSION);
    }

    /**
     * A utility method that returns a list of  {@link java.time.Instant} objects representing immutable versions
     * accessible from the OCFL Object represented by the session.
     *
     * @param objSession The OCFL object session
     * @return A list of Instant objects
     * @throws PersistentStorageException On read failure due to the session being closed or some other problem.
     */
    public static List<Instant> listVersions(final OCFLObjectSession objSession) throws PersistentStorageException {
        return  objSession.listVersions().stream().map(versionDetails -> versionDetails.getCreated().toInstant())
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
     * @return The path ( including the final slash ) to the internal Fedora directory within an OCFL object.
     */
    public static String getInternalFedoraDirectory() {
        return INTERNAL_FEDORA_DIRECTORY + File.separator;
    }


    /**
     * Mints an OCFL ID for the specified identifier
     * @param fedoraIdentifier The fedora identifier for the root OCFL object
     * @return The OCFL ID
     */
    public static String mintOCFLObjectId(final String fedoraIdentifier) {
        //TODO make OCFL Object Id minting more configurable.
        String bareFedoraIdentifier = fedoraIdentifier;
        if (fedoraIdentifier.indexOf(FEDORA_ID_PREFIX) == 0) {
            bareFedoraIdentifier = fedoraIdentifier.substring(FEDORA_ID_PREFIX.length());
        }

        //ensure no accidental collisions with the root ocfl identifier
        if (bareFedoraIdentifier.equals(DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID)) {
            throw new RepositoryRuntimeException(bareFedoraIdentifier + " is a reserved identifier");
        }

        bareFedoraIdentifier = bareFedoraIdentifier.replace("/", "_");

        if (bareFedoraIdentifier.length() == 0) {
            bareFedoraIdentifier = DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID;
        }

        log.debug("minted new ocfl object id:  {}", bareFedoraIdentifier);

        return bareFedoraIdentifier;
    }

    /**
     * Create a new ocfl repository
     * @param ocflStorageRootDir The ocfl storage root directory
     * @param ocflWorkDir The ocfl work directory
     * @return the repository
     */
    public static MutableOcflRepository createRepository(final File ocflStorageRootDir, final File ocflWorkDir) {
        ocflStorageRootDir.mkdirs();
        ocflWorkDir.mkdirs();
        return new OcflRepositoryBuilder()
                .layoutConfig(DefaultLayoutConfig.nTupleHashConfig())
                .storage((FileSystemOcflStorage.builder().repositoryRoot(ocflStorageRootDir.toPath()).build()))
                .workDir(ocflWorkDir.toPath())
                .buildMutable();
    }

}
