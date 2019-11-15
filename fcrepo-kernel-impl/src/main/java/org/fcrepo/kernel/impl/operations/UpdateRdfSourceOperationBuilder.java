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

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;

import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;


/**
 * Builder for operations to update rdf sources
 *
 * @author bbpennel
 * @author bseeger
 * @since 11/2019
 */

public class UpdateRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    /**
     * Holds the stream of user's triples.
     */
    private RdfStream tripleStream;

    /**
     * String of the resource ID.
     */
    private final String resourceId;

    /**
     * Constructor.
     *
     * @param resourceId the internal identifier.
     */
    public UpdateRdfSourceOperationBuilder(final String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public RdfSourceOperation build() {
        // TODO Perform triples validation, relaxed SMTs, etc here?
        return new UpdateRdfSourceOperation(this.resourceId, this.tripleStream);
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        tripleStream = triples;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final InputStream contentStream, final String mimetype) {
        final RdfStream stream;
        if (contentStream != null && mimetype != null) {
            final Model model = ModelFactory.createDefaultModel();
            final Lang lang = contentTypeToLang(mimetype);
            model.read(contentStream, this.resourceId, lang.getName().toUpperCase());
            stream = fromModel(ResourceFactory.createResource(this.resourceId).asNode(), model);
        } else {
            stream = null;
        }

        tripleStream = stream;
        return this;
    }
}
