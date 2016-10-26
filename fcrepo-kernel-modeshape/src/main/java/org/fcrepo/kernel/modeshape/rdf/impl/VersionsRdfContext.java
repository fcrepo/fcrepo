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

import static java.util.Calendar.getInstance;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_LABEL;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.modeshape.common.text.UrlEncoder;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import org.slf4j.Logger;


/**
 * {@link org.fcrepo.kernel.api.RdfStream} that supplies {@link Triple}s concerning
 * the versions of a selected {@link Node}.
 *
 * @author ajs6f
 * @since Oct 15, 2013
 */
public class VersionsRdfContext extends DefaultRdfStream {

    private final FedoraResource resource;

    private static final Logger LOGGER = getLogger(VersionsRdfContext.class);

    private static final UrlEncoder URL_ENCODER = new UrlEncoder();

    /**
     * Ordinary constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public VersionsRdfContext(final FedoraResource resource,
                              final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(idTranslator.reverse().convert(resource).asNode());
        this.resource = resource;
        concat(versionTriples());
    }

    @SuppressWarnings("unchecked")
    private Stream<Triple> versionTriples() {
        return resource.getVersionLabels()
            .flatMap(label -> {
                final Node versionSubject
                        = createProperty(topic() + "/" + FCR_VERSIONS + "/" + urlEncode(label)).asNode();

                // TODO - convert this to java.time.* classes once the kernel-api interface changes
                final Calendar cal = getInstance();
                cal.setTime(resource.getVersion(label).getCreatedDate());
                return of(
                        create(topic(), HAS_VERSION.asNode(), versionSubject),
                        create(versionSubject, HAS_VERSION_LABEL.asNode(), createLiteral(label)),
                        create(versionSubject, CREATED_DATE.asNode(), createTypedLiteral(cal).asNode()));
            });
    }

    private String urlEncode(final String string) {
        return URL_ENCODER.encode(string).replaceAll("[+]", "%20");
    }
}
