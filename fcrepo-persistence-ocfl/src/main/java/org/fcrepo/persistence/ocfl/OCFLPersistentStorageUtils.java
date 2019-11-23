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
package org.fcrepo.persistence.ocfl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.WriteOutcome;
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
     * Returns the relative subpath of the resourceId based on the ancestor's resource id.
     *
     * @param ancestorResourceId The ancestor resource
     * @param resourceId         The identifier of the resource whose subpath you wish to resolve.
     * @return The resolved subpath
     */
    public static String relativizeSubpath(final String ancestorResourceId, final String resourceId) {
        if (resourceId.equals(ancestorResourceId)) {
            return resourceId;
        } else if (resourceId.startsWith(ancestorResourceId)) {
            return resourceId.substring(ancestorResourceId.length() + 1);
        }

        throw new IllegalArgumentException(format("resource (%s) is not prefixed by ancestor resource (%s)", resourceId,
                ancestorResourceId));
    }

    /**
     * Returns the OCFL subpath for a given fedora subpath.  This returned subpath
     * does not include any added extendsions.
     * @param fedoraSubpath
     * @return
     */
    public static  String resolveOCFLSubpath(final String fedoraSubpath) {
        if (fedoraSubpath.endsWith(FEDORA_METADATA_SUFFIX)) {
            return fedoraSubpath.substring(0, fedoraSubpath.indexOf(FEDORA_METADATA_SUFFIX));
        } else {
            return fedoraSubpath;
        }
    }

    /**
     * Returns the RDF topic to be returned for a given resource identifier
     * For example:  passing info:fedora/resource1/fcr:metadata would return
     *  info:fedora/resource1 since  info:fedora/resource1 would be the expected
     *  topic.
     * @param fedoraIdentifier
     * @return
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
        try (final var  os = new ByteArrayOutputStream()) {
            final StreamRDF streamRDF = getWriterStream(os, getRdfFormat());
            streamRDF.start();
            triples.forEach(streamRDF::triple);
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
     * Get an RDF stream for the specified file.
     * @param identifier The resource identifier
     * @param version The version.  If null, the head state will be returned.
     * @param objSession The OCFL object session
     * @param subpath The path to the desired file.
     * @return
     * @throws PersistentStorageException
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
     */
    public static String resolveVersionId(final OCFLObjectSession objSession, final Instant version) {
        //TODO Implement resolution of a version id (OCFL-speak) from an instant (memento-speak)
       return null;
    }

    /**
     * Returns the RDF Format. By default NTRIPLES are returned.
     * @return
     */
    public static RDFFormat getRdfFormat(){
        return DEFAULT_RDF_FORMAT;
    }

    /**
     * Returns the RDF file extension.
     * @return
     */
    public static String getRDFFileExtension() {
        return "." + DEFAULT_RDF_FORMAT.getLang().getFileExtensions().get(0);
    }

    /**
     * The path ( including the final slash ) to the internal Fedora directory within an OCFL object.
     * @return
     */
    public static String getInternalFedoraDirectory() {
        return INTERNAL_FEDORA_DIRECTORY + File.separator;
    }

}
