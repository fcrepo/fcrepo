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
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static org.apache.jena.datatypes.xsd.impl.XSDDateTimeType.XSDdateTime;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.springframework.stereotype.Component;

/**
 * Retrieve the managed properties as triples
 *
 * @author dbernstein
 * @since 2020-01-07
 */
@Component
public class ManagedPropertiesServiceImpl implements ManagedPropertiesService {

    @Override
    public Stream<Triple> get(final FedoraResource resource) {
        final List<Triple> triples = new ArrayList<>();

        final var describedResource = resource.getDescribedResource();
        final var subject = createURI(resolveId(describedResource));

        var createdBy = resource.getCreatedBy();
        var createdDate = resource.getCreatedDate();
        var lastModifiedBy = resource.getLastModifiedBy();
        var lastModifiedDate = resource.getLastModifiedDate();

        if (describedResource instanceof Binary) {
            final Binary binary = (Binary) describedResource;

            createdBy = binary.getCreatedBy();
            createdDate = binary.getCreatedDate();
            lastModifiedBy = binary.getLastModifiedBy();
            lastModifiedDate = binary.getLastModifiedDate();

            triples.add(Triple.create(subject, HAS_SIZE.asNode(),
                    createLiteral(String.valueOf(binary.getContentSize()), XSDlong)));
            if (binary.getFilename() != null) {
                triples.add(Triple.create(subject, HAS_ORIGINAL_NAME.asNode(), createLiteral(binary.getFilename())));
            }
            if (binary.getMimeType() != null) {
                triples.add(Triple.create(subject, HAS_MIME_TYPE.asNode(), createLiteral(binary.getMimeType())));
            }
            if (binary.getContentDigests() != null) {
                for (var digest : binary.getContentDigests()) {
                    triples.add(Triple.create(subject, HAS_MESSAGE_DIGEST.asNode(),
                            createURI(digest.toString())));
                }

            }
        }

        triples.add(Triple.create(subject, CREATED_DATE.asNode(),
                                  createLiteral(createdDate.toString(), XSDdateTime)));
        triples.add(Triple.create(subject, LAST_MODIFIED_DATE.asNode(),
                                  createLiteral(lastModifiedDate.toString(), XSDdateTime)));
        if (createdBy != null) {
            triples.add(Triple.create(subject, CREATED_BY.asNode(), createLiteral(createdBy)));
        }
        if (lastModifiedBy != null) {
            triples.add(Triple.create(subject, LAST_MODIFIED_BY.asNode(), createLiteral(lastModifiedBy)));
        }

        describedResource.getSystemTypes(true).forEach(triple -> {
            triples.add(Triple.create(subject, type.asNode(), createURI(triple.toString())));
        });

        return new DefaultRdfStream(subject, triples.stream());
    }

    private String resolveId(final FedoraResource resource) {
        if (resource instanceof TimeMap) {
            return resource.getFedoraId().getFullId();
        }
        return resource.getId();
    }

}
