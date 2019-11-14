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

import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String INTERNAL_FEDORA_DIRECTORY = ".fcrepo";
    /**
     * The default RDF on disk format
     * TODO Make this value configurable
     */

    public static RDFFormat DEFAULT_RDF_FORMAT = NTRIPLES;
    /**
     * TODO Make this value configurable
     * The default extension for the rdf files.
     */
    public static String DEFAULT_RDF_EXTENSION = ".n3";

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
        try (final PipedOutputStream os = new PipedOutputStream()) {
            final PipedInputStream is = new PipedInputStream();
            os.connect(is);
            final StreamRDF streamRDF = getWriterStream(os, DEFAULT_RDF_FORMAT);
            streamRDF.start();
            triples.forEach(streamRDF::triple);
            streamRDF.finish();
            session.write(subpath + DEFAULT_RDF_EXTENSION, is);
            log.debug("wrote {} to {}", subpath, session);
        } catch (final IOException ex) {
            throw new PersistentStorageException(format("failed to write subpath %s in %s", subpath, session), ex);
        }
    }
}
