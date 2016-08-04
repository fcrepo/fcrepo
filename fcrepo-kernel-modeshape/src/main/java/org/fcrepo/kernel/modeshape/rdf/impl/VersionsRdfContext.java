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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.modeshape.utils.UncheckedFunction;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import org.slf4j.Logger;


/**
 * {@link org.fcrepo.kernel.api.RdfStream} that supplies {@link Triple}s concerning
 * the versions of a selected {@link Node}.
 *
 * @author ajs6f
 * @since Oct 15, 2013
 */
public class VersionsRdfContext extends DefaultRdfStream {

    private final VersionHistory versionHistory;

    private static final Logger LOGGER = getLogger(VersionsRdfContext.class);

    /**
     * Ordinary constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */
    public VersionsRdfContext(final FedoraResource resource,
                              final Converter<Resource, String> idTranslator)
        throws RepositoryException {
        super(resource.asUri(idTranslator).asNode());
        this.versionHistory = resource.getVersionHistory();
        concat(versionTriples());
    }

    @SuppressWarnings("unchecked")
    private Stream<Triple> versionTriples() throws RepositoryException {
        return iteratorToStream(versionHistory.getAllVersions())
            /* Discard jcr:rootVersion */
            .filter(uncheck((final Version v) -> !v.getName().equals(versionHistory.getRootVersion().getName())))
            /* Omit unlabelled versions */
            .filter(uncheck((final Version v) -> {
                final String[] labels = versionHistory.getVersionLabels(v);
                if (labels.length == 0) {
                    LOGGER.warn("An unlabeled version for {} was found!  Omitting from version listing!",
                        topic().getURI());
                } else if (labels.length > 1) {
                    LOGGER.info("Multiple version labels found for {}!  Using first label, \"{}\".", topic().getURI());
                }
                return labels.length > 0;
            }))
            .flatMap(UncheckedFunction.uncheck((final Version v) -> {
                final String[] labels = versionHistory.getVersionLabels(v);
                final Node versionSubject
                        = createProperty(topic() + "/" + FCR_VERSIONS + "/" + labels[0]).asNode();

                return Stream.concat(
                        Arrays.stream(labels).map(x -> create(versionSubject, HAS_VERSION_LABEL.asNode(),
                                createLiteral(x))),

                        Stream.of(create(topic(), HAS_VERSION.asNode(), versionSubject),
                            create(versionSubject, CREATED_DATE.asNode(),
                                createTypedLiteral(v.getCreated()).asNode())));
            }));
    }
}
