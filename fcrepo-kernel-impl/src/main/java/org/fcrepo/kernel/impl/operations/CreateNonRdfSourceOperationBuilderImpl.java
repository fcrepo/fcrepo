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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;


/**
 * Builder for operations to create new non-rdf sources
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperationBuilderImpl extends AbstractNonRdfSourceOperationBuilder
        implements CreateNonRdfSourceOperationBuilder {

    private FedoraId parentId;

    /**
     * Constructor for external binary.
     *
     * @param rescId      the internal identifier
     * @param handling    the external content handling type.
     * @param externalUri the external content URI.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final FedoraId rescId, final String handling,
            final URI externalUri) {
        super(rescId, handling, externalUri);
    }

    /**
     * Constructor for internal binary.
     *
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final FedoraId rescId, final InputStream stream) {
        super(rescId, stream);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder mimeType(final String mimeType) {
        return (CreateNonRdfSourceOperationBuilder) super.mimeType(mimeType);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder filename(final String filename) {
        return (CreateNonRdfSourceOperationBuilder) super.filename(filename);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentDigests(final Collection<URI> digests) {
        return (CreateNonRdfSourceOperationBuilder) super.contentDigests(digests);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentSize(final long size) {
        return (CreateNonRdfSourceOperationBuilder) super.contentSize(size);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        return (CreateNonRdfSourceOperationBuilder) super.userPrincipal(userPrincipal);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder parentId(final FedoraId parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperation build() {
        final CreateNonRdfSourceOperation operation;
        if (externalURI != null && externalType != null) {
            operation = new CreateNonRdfSourceOperation(resourceId, externalURI, externalType);
        } else {
            operation = new CreateNonRdfSourceOperation(resourceId, content);
        }

        operation.setUserPrincipal(userPrincipal);
        operation.setDigests(digests);
        operation.setFilename(filename);
        operation.setContentSize(contentSize);
        operation.setMimeType(mimeType);
        operation.setParentId(parentId);

        return operation;
    }
}
