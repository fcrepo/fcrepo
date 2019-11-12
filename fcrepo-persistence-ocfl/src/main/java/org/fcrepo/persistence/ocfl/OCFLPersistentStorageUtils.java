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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
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

    /**
     * Returns the relative subpath of the resourceId based on the ancestor's resource id.
     *
     * @param ancestorResourceId The ancestor resource
     * @param resourceId         The identifier of the resource whose subpath you wish to resolve.
     * @return The resolved subpath
     */
    public static String relativizeSubpath(final String ancestorResourceId, final String resourceId) {
        if (resourceId.startsWith(ancestorResourceId)) {
            return resourceId.substring(ancestorResourceId.length() + 1);
        }

        throw new IllegalArgumentException(format("resource (%s) is not prefixed by ancestor resource (%s)", resourceId,
                ancestorResourceId));
    }

    /**
     * Writes an RDFStream to a subpath within an ocfl object.
     *
     * @param session The object session
     * @param triples The triples
     * @param subpath The subpath within the OCFL Object
     * @throws PersistentStorageException on write failure
     */
    public static void writeRDF(final OCFLObjectSession session, final RdfStream triples, final String subpath)
            throws PersistentStorageException {
        try (final var  os = new ByteArrayOutputStream()) {
            final StreamRDF streamRDF = getWriterStream(os, getRdfFormat());
            streamRDF.start();
            triples.forEach(streamRDF::triple);
            streamRDF.finish();

            final var is = new ByteArrayInputStream(os.toByteArray());
            session.write(subpath + getRDFFileExtension(), is);
            log.debug("wrote {} to {}", subpath, session);
        } catch (final IOException ex) {
            throw new PersistentStorageException(format("failed to write subpath %s in %s", subpath, session), ex);
        }
    }

    private static InputStream readFile(final String version,  final OCFLObjectSession objSession, final String filename)
            throws PersistentStorageException {
        return version == null ? objSession.read(filename) : objSession.read(filename, version);
    }

    /**
     * Get an RDF stream for the specified file.
     * @param identifier The resource identifier
     * @param version The version.  If null, the head state will be returned.
     * @param objSession The OCFL object session
     * @param filePath The path to the desired file.
     * @return
     * @throws PersistentStorageException
     */
    public static RdfStream getRdfStream(final String identifier, final Instant version,
                                         final OCFLObjectSession objSession,
                                         final String filePath) throws PersistentStorageException {

        final String versionNumber = resolveVersionId(objSession, version);
        try (final InputStream is = readFile(null, objSession, filePath)) {
            final Model model = createDefaultModel();
            RDFDataMgr.read(model, is, DEFAULT_RDF_FORMAT.getLang());
            return new DefaultRdfStream(createURI(identifier),
                    model.listStatements().toList().stream().map(Statement::asTriple));
        } catch (IOException ex) {
            throw new PersistentStorageException(format("unable to read %s ;  version = %s", identifier, version), ex);
        }
    }

    private static  String resolveVersionId(final OCFLObjectSession objSession, final Instant version) {
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
