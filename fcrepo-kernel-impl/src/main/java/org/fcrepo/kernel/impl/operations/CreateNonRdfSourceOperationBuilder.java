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
import java.net.URI;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;


/**
 * Builder for operations to create new non-rdf sources
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperationBuilder extends AbstractNonRdfSourceOperationBuilder implements NonRdfSourceOperationBuilder {


    /**
     * Constructor for external binary.
     *
     * @param rescId      the internal identifier
     * @param handling    the external content handling type.
     * @param externalUri the external content URI.
     */
    protected CreateNonRdfSourceOperationBuilder(final String rescId, final String handling, final URI externalUri) {
        this(rescId);
        this.externalURI = externalUri;
        this.externalType = handling;
    }

    /**
     * Constructor for internal binary.
     *
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected CreateNonRdfSourceOperationBuilder(final String rescId, final InputStream stream) {
        this(rescId);
        this.content = stream;
    }

    protected CreateNonRdfSourceOperationBuilder(final String rescId) {
        super(rescId);
    }

    @Override
    public CreateNonRdfSourceOperation build() {
        if (externalURI != null && externalType != null) {
            return new CreateNonRdfSourceOperation(this.resourceId, this.externalURI, this.externalType,
                    this.mimeType, this.filename, this.digests);
        } else {
            return new CreateNonRdfSourceOperation(this.resourceId, this.content, this.mimeType, this.contentSize,
                    this.filename, this.digests);
        }
    }

}
