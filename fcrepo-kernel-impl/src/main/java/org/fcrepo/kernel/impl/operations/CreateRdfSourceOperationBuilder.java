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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;


/**
 * Builder for operations to create rdf sources
 *
 * @author bbpennel
 */
public class CreateRdfSourceOperationBuilder extends AbstractRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    private RdfStream tripleStream;

    /**
     * Constructor.
     *
     * @param resourceId the internal identifier.
     */
    public CreateRdfSourceOperationBuilder(final String resourceId) {
        super(resourceId);
    }

    @Override
    public RdfSourceOperation build() {
        // TODO Perform triples validation, relaxed SMTs, etc
        return null;
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        this.tripleStream = triples;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final InputStream contentStream, final String mimetype) {
        final Model model = ModelFactory.createDefaultModel();
        final Lang lang = contentTypeToLang(mimetype);
        model.read(contentStream, this.resourceId, lang.getName().toUpperCase());
        final List<Triple> triples = new ArrayList<>();
        model.listStatements().forEachRemaining(p ->
            triples.add(new Triple(p.getSubject().asNode(), p.getPredicate().asNode(), p.getObject().asNode()))
        );
        this.tripleStream = new DefaultRdfStream(this.resourceNode, triples.stream());
        return this;
    }
}
