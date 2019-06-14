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

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TIME_MAP;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalType;

import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/1/14
 */
public class TypeRdfContext extends NodeRdfContext {

    /**
     * Full URI for internal fedora:TimeMap type.
     */
    private static URI fedoraTimemap = URI.create(FEDORA_TIME_MAP.replace("fedora:", REPOSITORY_NAMESPACE));

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public TypeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        final List<URI> typeStream =
                resource.getTypes().stream().filter(isInternalType.negate()).collect(Collectors.toList());

        concat(typeStream.stream().map(uri -> create(subject(), type.asNode(), createURI(uri.toString()))));

        // If has rdf:type fedora:TimeMap and no memento:TimeMap, add a memento:TimeMap type.
        // https://jira.duraspace.org/browse/FCREPO-3006
        final boolean hasTimeMap = typeStream.stream().anyMatch(uri -> uri.equals(fedoraTimemap)) &&
                typeStream.stream().noneMatch(uri -> uri.equals(URI.create(VERSIONING_TIMEMAP_TYPE)));
        if (hasTimeMap) {
            concat(of(create(subject(), type.asNode(), createURI(VERSIONING_TIMEMAP_TYPE))));
        }
    }
}
