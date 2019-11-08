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
package org.fcrepo.kernel.impl.operations;

import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Builder for operations to create rdf sources
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    /**
     * Holds the stream of user's triples.
     */
    private RdfStream tripleStream;

    /**
     * String of the resource ID.
     */
    private String resourceId;

    /**
     * Constructor.
     *
     * @param resourceId the internal identifier.
     */
    public CreateRdfSourceOperationBuilder(final String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public RdfSourceOperation build() {
        return new CreateRdfSourceOperation(this.resourceId, tripleStream);
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        this.tripleStream = triples;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final InputStream contentStream, final String mimetype) {
        if (contentStream != null && mimetype != null) {
            final Model model = ModelFactory.createDefaultModel();
            final Lang lang = contentTypeToLang(mimetype);
            model.read(contentStream, this.resourceId, lang.getName().toUpperCase());
            final List<Triple> triples = new ArrayList<>();
            model.listStatements().forEachRemaining(p ->
                    triples.add(new Triple(p.getSubject().asNode(), p.getPredicate().asNode(), p.getObject().asNode()))
            );
            this.tripleStream = new DefaultRdfStream(ResourceFactory.createResource(this.resourceId).asNode(),
                    triples.stream());
        } else {
            this.tripleStream = null;
        }
        return this;
    }
}
